/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.genlex.ccg.unification;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.ILossDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.IUnificationSplitter;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYLexicalStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Unification-based GENLEX method. See Kwiatkowski et al. 2010 for details.
 * 
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 */
public class UnificationGenlex<DI extends SingleSentence>
		implements
		ILexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {
	
	public static final ILogger							LOG							= LoggerFactory
																							.create(UnificationGenlex.class);
	
	public static final String							SPLITTING_LEXICAL_ORIGIN	= "splitting";
	private final boolean								conservative;
	private final AbstractCKYParser<LogicalExpression>	parser;
	
	private final IUnificationSplitter					splitter;
	
	public UnificationGenlex(AbstractCKYParser<LogicalExpression> parser,
			IUnificationSplitter splitter, boolean conservative) {
		this.parser = parser;
		this.splitter = splitter;
		this.conservative = conservative;
	}
	
	@Override
	public ILexicon<LogicalExpression> generate(final DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {
		
		// Parse the sentence with pruner
		final CKYParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem.getSample(), createPruningFilter(dataItem),
				model.createDataItemModel(dataItem.getSample()));
		LOG.info("Lexical generation parsing time %f",
				parserOutput.getParsingTime() / 1000.0);
		
		// If no correct parses exist, create a new lexical entry for the
		// complete sentence and return a lexicon contaning only it
		final List<? extends IDerivation<LogicalExpression>> correctParses = parserOutput
				.getMaxParses(new IFilter<LogicalExpression>() {
					
					@Override
					public boolean isValid(LogicalExpression e) {
						return dataItem.getLabel().equals(e);
					}
				});
		if (correctParses.isEmpty()) {
			LOG.info("No correct parses generated for unification GENLEX.");
			return new Lexicon<LogicalExpression>(
					CollectionUtils
							.singletonSetOf(new LexicalEntry<LogicalExpression>(
									dataItem.getSample().getTokens(),
									categoryServices.getSentenceCategory()
											.cloneWithNewSemantics(
													dataItem.getLabel()),
									SPLITTING_LEXICAL_ORIGIN)));
		} else if (correctParses.size() > 1) {
			LOG.info("Best parses ambiguity, returning no generated lexical entries");
			return new Lexicon<LogicalExpression>();
		}
		
		final Chart<LogicalExpression> chart = parserOutput.getChart();
		
		// TODO [yoav] [feature] If we want to support merge operations, we
		// should insert
		// it here.
		
		// Iterate over each nonterminal in the highest scoring parse in the
		// chart to collect potential splits
		final List<Split> splits = new LinkedList<Split>();
		chart.setMaxes(dataItem.getLabel());
		double mostImproved = 0.0;
		final int size = dataItem.getSample().getTokens().size();
		for (int begin = 0; begin < size; begin++) {
			for (int len = 0; len < size - begin; len++) {
				final Iterator<Cell<LogicalExpression>> i = chart
						.getSpanIterator(begin, begin + len);
				while (i.hasNext()) {
					final Cell<LogicalExpression> cell = i.next();
					if (cell.getIsMax()) {
						mostImproved = collectBestSplits(cell, begin, begin
								+ len, splits, chart, dataItem, mostImproved,
								model);
					}
				}
			}
		}
		
		// Collect the new lexical entries
		final Set<LexicalEntry<LogicalExpression>> entries = new HashSet<LexicalEntry<LogicalExpression>>();
		
		if (!conservative || splits.size() == 1) {
			for (final Split split : splits) {
				LOG.info("Split: %s", split);
				if (!split.reusingRight && split.right.hasLexicalMaxStep()) {
					entries.addAll(split.right.getViterbiLexicalEntries());
				}
				if (!split.reusingLeft && split.left.hasLexicalMaxStep()) {
					entries.addAll(split.left.getViterbiLexicalEntries());
				}
				
				// NOTE: In the original version of UBL the root of the new
				// split was removed. However, this version doesn't support
				// this. The root lexical entry remains in the model. It's not
				// likely to be used, at least not immediately, since it's
				// scored lower than the new entries (this is how they were
				// selected).
			}
		} else {
			LOG.debug("Skipped addings splits due to %d ties", splits.size());
			for (final Split split : splits) {
				LOG.info(split);
			}
			
		}
		
		LOG.debug("Generated lexicon size: %d", entries.size());
		
		return new Lexicon<LogicalExpression>(entries);
	}
	
	/**
	 * For a given cell, create all possible splits and take the ones with the
	 * maximum score increase.
	 */
	private double collectBestSplits(Cell<LogicalExpression> cell, int begin,
			int end, List<Split> maxEntries, Chart<LogicalExpression> chart,
			ILabeledDataItem<Sentence, LogicalExpression> dataItem,
			double mostImproved,
			IModelImmutable<Sentence, LogicalExpression> model) {
		
		// Cell category and tokens
		final Category<LogicalExpression> rootCategory = cell.getCategory();
		final List<String> rootTokens = dataItem.getSample().getTokens()
				.subList(cell.getStart(), cell.getEnd() + 1);
		
		// Score of best split found so far
		double currentMaxImprovement = mostImproved;
		
		// Cell factory to create new cells
		final AbstractCellFactory<LogicalExpression> cellFactory = chart
				.getCellFactory();
		
		// Get all splits for the root category
		final Set<SplittingPair> allSplits = splitter.getSplits(rootCategory);
		
		// Iterate over all possible splits
		for (final SplittingPair split : allSplits) {
			final Category<LogicalExpression> left = split.getLeft();
			final Category<LogicalExpression> right = split.getRight();
			
			// Iterate over all breaking points for dividing the phrase
			// (tokens), create new lexical entries and cells
			for (int splittingPoint = begin; splittingPoint < end; splittingPoint++) {
				// Create new lexical entries
				
				// TODO [yoav] [limitation] Factoring so templates and lexemes
				// participating
				// can be scored by the relevant feature sets. However, this
				// gives only the maximal factoring, which misses the option of
				// adding non maximal ones.
				final LexicalEntry<LogicalExpression> leftEntry = FactoredLexicon
						.factor(new LexicalEntry<LogicalExpression>(
								CollectionUtils.subList(rootTokens, 0,
										(splittingPoint - begin) + 1), left,
								SPLITTING_LEXICAL_ORIGIN));
				final LexicalEntry<LogicalExpression> rightEntry = FactoredLexicon
						.factor(new LexicalEntry<LogicalExpression>(
								CollectionUtils.subList(rootTokens,
										(splittingPoint - begin) + 1,
										rootTokens.size()), right,
								SPLITTING_LEXICAL_ORIGIN));
				
				// If both created lexical entries exist in the model, skip this
				// split
				if (model.getLexicon().contains(leftEntry)
						&& model.getLexicon().contains(rightEntry)) {
					continue;
				}
				
				// NOTE: we do not add the cell to the chart below. this is
				// because we will be doing lots of splits and evaluating how
				// much each would help on the same chart, without actually
				// adding each potential option (or rebuilding the chart each
				// time, etc).
				
				final IDataItemModel<LogicalExpression> dataItemModel = model
						.createDataItemModel(dataItem.getSample());
				
				// Create cells using the new lexical entries
				final Cell<LogicalExpression> newLeftCell = cellFactory.create(
						new CKYLexicalStep<LogicalExpression>(leftEntry, false,
								dataItemModel), begin, splittingPoint);
				final Cell<LogicalExpression> newRightCell = cellFactory
						.create(new CKYLexicalStep<LogicalExpression>(
								rightEntry, false, dataItemModel),
								splittingPoint + 1, end);
				
				// If equivalent cells exist in the chart and they have a higher
				// max score, reuse them
				final Cell<LogicalExpression> leftCell;
				final boolean reusingLeft;
				final Cell<LogicalExpression> chartLeftCell = chart
						.getCell(newLeftCell);
				if (chartLeftCell == null
						|| chartLeftCell.getViterbiScore() >= newLeftCell
								.getViterbiScore()) {
					leftCell = newLeftCell;
					reusingLeft = false;
				} else {
					leftCell = chartLeftCell;
					reusingLeft = true;
				}
				
				final Cell<LogicalExpression> rightCell;
				final boolean reusingRight;
				final Cell<LogicalExpression> chartRightCell = chart
						.getCell(newRightCell);
				if (chartRightCell == null
						|| chartRightCell.getViterbiScore() >= newRightCell
								.getViterbiScore()) {
					rightCell = newRightCell;
					reusingRight = false;
				} else {
					rightCell = chartRightCell;
					reusingRight = true;
				}
				
				// Only consider this split if it adds a new lexical entry
				if ((reusingLeft || model.getLexicon().contains(leftEntry))
						&& (reusingRight || model.getLexicon().contains(
								rightEntry))) {
					continue;
				}
				
				// Create the new root cell
				final Cell<LogicalExpression> newRootCell = cellFactory.create(
						new CKYParseStep<LogicalExpression>(rootCategory,
								leftCell, rightCell, cell.isFullParse(),
								RuleName.create("splitMerge", null),
								dataItemModel), leftCell.getStart(), rightCell
								.getEnd());
				
				// Compute the score improvement
				final double improvement = newRootCell.getViterbiScore()
						- cell.getViterbiScore();
				
				// Create the split object
				final Split splitCell = new Split(leftCell, reusingLeft,
						rightCell, reusingRight, cell, improvement);
				LOG.debug("Split under consideration:\n%s", splitCell);
				
				// If the score is higher, retain this split. Adding to the
				// chart will be done later.
				if (improvement >= currentMaxImprovement) {
					if (improvement > currentMaxImprovement) {
						maxEntries.clear();
						currentMaxImprovement = improvement;
					}
					maxEntries.add(splitCell);
				}
			}
		}
		return currentMaxImprovement;
	}
	
	/**
	 * Create a pruning filter for parsing for the given data item.
	 * 
	 * @param dataItem
	 * @return
	 */
	private IFilter<LogicalExpression> createPruningFilter(
			final IDataItem<Sentence> dataItem) {
		if (dataItem instanceof ILossDataItem) {
			return new IFilter<LogicalExpression>() {
				
				@SuppressWarnings("unchecked")
				@Override
				public boolean isValid(LogicalExpression e) {
					return !((ILossDataItem<Sentence, LogicalExpression>) dataItem)
							.prune(e);
				}
			};
		} else {
			return new IFilter<LogicalExpression>() {
				@Override
				public boolean isValid(LogicalExpression e) {
					return true;
				}
			};
		}
	}
	
	public static class Creator<DI extends SingleSentence> implements
			IResourceObjectCreator<UnificationGenlex<DI>> {
		
		private final String	type;
		
		public Creator() {
			this("genlex.unification");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public UnificationGenlex<DI> create(Parameters params,
				IResourceRepository repo) {
			return new UnificationGenlex<DI>(
					(AbstractCKYParser<LogicalExpression>) repo
							.getResource(ParameterizedExperiment.PARSER_RESOURCE),
					(IUnificationSplitter) repo.getResource(params
							.get("splitter")), "true".equals(params
							.get("conservative")));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, UnificationGenlex.class)
					.addParam("splitter", "IUnificationSplitter",
							"Category splitter")
					.addParam("convervative", "boolean",
							"If 'true' only use splits if there's a single best split.")
					.build();
		}
		
	}
	
	private static class Split {
		private final Cell<LogicalExpression>	left;
		private final Cell<LogicalExpression>	original;
		private final boolean					reusingLeft;
		private final boolean					reusingRight;
		private final Cell<LogicalExpression>	right;
		private final double					scoreImprovement;
		
		public Split(Cell<LogicalExpression> left, boolean reusingLeft,
				Cell<LogicalExpression> right, boolean reusingRight,
				Cell<LogicalExpression> original, double scoreImprovement) {
			this.left = left;
			this.reusingLeft = reusingLeft;
			this.reusingRight = reusingRight;
			this.original = original;
			this.right = right;
			this.scoreImprovement = scoreImprovement;
		}
		
		private static String cellToString(Cell<LogicalExpression> cell) {
			return new StringBuilder().append("[").append(cell.getStart())
					.append(", ").append(cell.getEnd()).append("] ")
					.append(cell.hasLexicalMaxStep() ? " (LEX) " : "")
					.append(cell.getCategory()).toString();
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("[").append(
					scoreImprovement).append("] ");
			sb.append(cellToString(original)).append('\n');
			
			sb.append("\tL: ");
			if (reusingLeft) {
				sb.append("[reusing] ");
			}
			sb.append(cellToString(left)).append('\n');
			
			sb.append("\tR: ");
			if (reusingRight) {
				sb.append("[reusing] ");
			}
			sb.append(cellToString(right));
			
			return sb.toString();
		}
		
	}
}
