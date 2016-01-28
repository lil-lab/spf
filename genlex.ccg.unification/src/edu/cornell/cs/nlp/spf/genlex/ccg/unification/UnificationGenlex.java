/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.genlex.ccg.unification;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGeneratorPrecise;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.IUnificationSplitter;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.AbstractCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.AbstractCellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.CKYLexicalStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.CKYParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.WeightedCKYLexicalStep;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.steps.WeightedCKYParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.filter.IParsingFilterFactory;
import edu.cornell.cs.nlp.spf.parser.filter.StubFilterFactory;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Unification-based GENLEX method. See Kwiatkowski et al. 2010 for details.
 *
 * @author Yoav Artzi
 * @author Luke Zettlemoyer
 */
public class UnificationGenlex<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
		extends
		AbstractLexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>>
		implements
		ILexiconGeneratorPrecise<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {

	public static final ILogger									LOG					= LoggerFactory
			.create(UnificationGenlex.class);

	private static final long									serialVersionUID	= -361969526470310080L;

	private final boolean										conservative;
	private final IParsingFilterFactory<DI, LogicalExpression>	filterFactory;

	private final AbstractCKYParser<SAMPLE, LogicalExpression>	parser;

	private final Syntax										sentenceSyntax;

	private final IUnificationSplitter							splitter;

	public UnificationGenlex(
			AbstractCKYParser<SAMPLE, LogicalExpression> parser,
			IUnificationSplitter splitter, boolean conservative,
			IParsingFilterFactory<DI, LogicalExpression> filterFactory,
			Syntax sentenceSyntax, String origin) {
		super(origin, false);
		this.parser = parser;
		this.splitter = splitter;
		this.conservative = conservative;
		this.filterFactory = filterFactory;
		this.sentenceSyntax = sentenceSyntax;
	}

	@Override
	public ILexicon<LogicalExpression> generate(final DI dataItem,
			IModelImmutable<Sentence, LogicalExpression> model,
			ICategoryServices<LogicalExpression> categoryServices) {

		// Parse the sentence with pruner
		final CKYParserOutput<LogicalExpression> parserOutput = parser.parse(
				dataItem.getSample(), filterFactory.create(dataItem),
				model.createDataItemModel(dataItem.getSample()));
		LOG.info("Lexical generation parsing time %f",
				parserOutput.getParsingTime() / 1000.0);

		// If no correct parses exist, create a new lexical entry for the
		// complete sentence and return a lexicon containing only it.
		final List<? extends IDerivation<LogicalExpression>> correctParses = parserOutput
				.getMaxDerivations(
						e -> dataItem.getLabel().equals(e.getSemantics()));
		if (correctParses.isEmpty()) {
			LOG.info("No correct parses generated for unification GENLEX.");
			return new Lexicon<LogicalExpression>(CollectionUtils
					.singletonSetOf(new LexicalEntry<LogicalExpression>(
							dataItem.getSample().getTokens(),
							Category.create(sentenceSyntax,
									dataItem.getLabel()),
							false, entryProperties)));
		} else if (correctParses.size() > 1) {
			LOG.info(
					"Best parses ambiguity, returning no generated lexical entries");
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
					if (cell.isMax()) {
						mostImproved = collectBestSplits(cell, begin,
								begin + len, splits, chart, dataItem,
								mostImproved, model);
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

	@Override
	public void init(IModelImmutable<Sentence, LogicalExpression> model) {
		// Nothing to do. Unification GENLEX is not influenced by the content of
		// the model (including lexemes and templates).
	}

	@Override
	public boolean isGenerated(LexicalEntry<LogicalExpression> entry) {
		return origin.equals(entry.getOrigin());
	}

	/**
	 * For a given cell, create all possible splits and take the ones with the
	 * maximum score increase.
	 */
	private double collectBestSplits(Cell<LogicalExpression> cell, int begin,
			int end, List<Split> maxEntries, Chart<LogicalExpression> chart,
			DI dataItem, double mostImproved,
			IModelImmutable<Sentence, LogicalExpression> model) {

		// Cell category and tokens
		final Category<LogicalExpression> rootCategory = cell.getCategory();
		final TokenSeq rootTokens = dataItem.getSample().getTokens()
				.sub(cell.getStart(), cell.getEnd() + 1);

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
				final LexicalEntry<LogicalExpression> leftEntry = FactoringServices
						.factor(new LexicalEntry<LogicalExpression>(
								rootTokens.sub(0, splittingPoint - begin + 1),
								left, false, entryProperties));
				final LexicalEntry<LogicalExpression> rightEntry = FactoringServices
						.factor(new LexicalEntry<LogicalExpression>(
								rootTokens.sub(splittingPoint - begin + 1,
										rootTokens.size()),
								right, false, entryProperties));

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
				final Cell<LogicalExpression> newLeftCell = cellFactory
						.create(new WeightedCKYLexicalStep<LogicalExpression>(
								new CKYLexicalStep<LogicalExpression>(leftEntry,
										false, begin, splittingPoint),
								dataItemModel));
				final Cell<LogicalExpression> newRightCell = cellFactory
						.create(new WeightedCKYLexicalStep<LogicalExpression>(
								new CKYLexicalStep<LogicalExpression>(
										rightEntry, false, splittingPoint + 1,
										end),
								dataItemModel));

				// If equivalent cells exist in the chart and they have a higher
				// max score, reuse them
				final Cell<LogicalExpression> leftCell;
				final boolean reusingLeft;
				final Cell<LogicalExpression> chartLeftCell = chart
						.getCell(newLeftCell);
				if (chartLeftCell == null || chartLeftCell
						.getViterbiScore() >= newLeftCell.getViterbiScore()) {
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
				if (chartRightCell == null || chartRightCell
						.getViterbiScore() >= newRightCell.getViterbiScore()) {
					rightCell = newRightCell;
					reusingRight = false;
				} else {
					rightCell = chartRightCell;
					reusingRight = true;
				}

				// Only consider this split if it adds a new lexical entry
				if ((reusingLeft || model.getLexicon().contains(leftEntry))
						&& (reusingRight
								|| model.getLexicon().contains(rightEntry))) {
					continue;
				}

				// Create the new root cell
				final Cell<LogicalExpression> newRootCell = cellFactory
						.create(new WeightedCKYParseStep<LogicalExpression>(
								new CKYParseStep<LogicalExpression>(
										rootCategory, leftCell, rightCell,
										cell.isFullParse(),
										RuleName.create("splitMerge", null),
										leftCell.getStart(),
										rightCell.getEnd()),
								dataItemModel));

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

	public static class Creator<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
			implements IResourceObjectCreator<UnificationGenlex<SAMPLE, DI>> {

		private final String type;

		public Creator() {
			this("genlex.unification");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public UnificationGenlex<SAMPLE, DI> create(Parameters params,
				IResourceRepository repo) {
			return new UnificationGenlex<SAMPLE, DI>(
					(AbstractCKYParser<SAMPLE, LogicalExpression>) repo
							.get(ParameterizedExperiment.PARSER_RESOURCE),
					(IUnificationSplitter) repo.get(params.get("splitter")),
					"true".equals(params.get("conservative")),
					(IParsingFilterFactory<DI, LogicalExpression>) (params
							.contains("filterFactory")
									? repo.get(params.get("filterFactory"))
									: new StubFilterFactory<DI, LogicalExpression>()),
					Syntax.read(params.get("sentenceSyntax")),
					params.get("origin", "splitting"));
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
					.addParam("filterFactory", IParsingFilterFactory.class,
							"Factory to create parsing filters (optional).")
					.addParam("origin", String.class,
							"Origin of generated entries (default: splitting)")
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
			final StringBuilder sb = new StringBuilder("[")
					.append(scoreImprovement).append("] ");
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
