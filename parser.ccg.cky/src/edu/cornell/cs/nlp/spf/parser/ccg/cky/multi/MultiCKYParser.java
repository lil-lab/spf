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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.multi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.google.common.base.Function;

import edu.cornell.cs.nlp.spf.base.concurrency.ITinyExecutor;
import edu.cornell.cs.nlp.spf.base.concurrency.Shutdownable;
import edu.cornell.cs.nlp.spf.base.concurrency.TinyExecutorService;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ISentenceLexiconGenerator;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.AbstractCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYBinaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYUnaryParsingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.AbstractCellFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.AggressiveWordSkippingLexicalGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.BackwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.ForwardSkippingRule;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.sloppy.SimpleWordSkippingLexicalGenerator;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.unaryconstraint.UnaryConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ILexicalRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.LexicalRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleSet;
import edu.cornell.cs.nlp.utils.collections.SetUtils;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.filter.FilterUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.log.thread.LoggingRunnable;
import edu.cornell.cs.nlp.utils.log.thread.LoggingThreadFactory;

/**
 * Multi threaded CKY parser. Work is distributed on the level of span splits.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item type.
 * @param <MR>
 *            type of semantics.
 */
public class MultiCKYParser<DI extends Sentence, MR>
		extends AbstractCKYParser<DI, MR> implements Shutdownable {
	public static final ILogger		LOG					= LoggerFactory
			.create(MultiCKYParser.class);
	private static final long		serialVersionUID	= 8447853586348529473L;
	private transient ITinyExecutor	executor;
	private final Integer			numThreads;
	private final boolean			preChartPruning;
	private final String			threadNamePrefix;

	private MultiCKYParser(int maxNumberOfCellsInSpan,
			CKYBinaryParsingRule<MR>[] binaryRules,
			List<ISentenceLexiconGenerator<DI, MR>> sentenceLexiconGenerators,
			List<ISentenceLexiconGenerator<DI, MR>> sloppyLexicalGenerators,
			ICategoryServices<MR> categoryServices, boolean pruneLexicalCells,
			boolean preChartPruning, IFilter<Category<MR>> completeParseFilter,
			CKYUnaryParsingRule<MR>[] unaryRules, Integer numThreads,
			String threadNamePrefix, ILexicalRule<MR> lexicalRule,
			boolean breakTies) {
		super(maxNumberOfCellsInSpan, binaryRules, sentenceLexiconGenerators,
				sloppyLexicalGenerators, categoryServices, pruneLexicalCells,
				completeParseFilter, unaryRules, lexicalRule, breakTies);
		this.numThreads = numThreads;
		this.threadNamePrefix = threadNamePrefix;
		this.executor = new TinyExecutorService(
				numThreads == null ? Runtime.getRuntime().availableProcessors()
						: numThreads,
				new LoggingThreadFactory(threadNamePrefix),
				ITinyExecutor.DEFAULT_MONITOR_SLEEP);
		this.preChartPruning = preChartPruning;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}

	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	@Override
	public void shutdown() {
		executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}

	/**
	 * @param ois
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		this.executor = new TinyExecutorService(
				numThreads == null ? Runtime.getRuntime().availableProcessors()
						: numThreads,
				new LoggingThreadFactory(threadNamePrefix),
				ITinyExecutor.DEFAULT_MONITOR_SLEEP);
	}

	@Override
	protected Chart<MR> doParse(Predicate<ParsingOp<MR>> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			ILexiconImmutable<MR> lexicon) {

		LOG.debug(
				"Starting a multi-threaded CKY parse (chart already populated)");

		// Generate all triplets of span and splits <begin,end,split> and
		// store them in a set. Notice the added dummy splits for lexical jobs.
		final Map<SpanPair, Set<SplitTriplet>> spans = new HashMap<SpanPair, Set<SplitTriplet>>();
		for (int len = 0; len < numTokens; len++) {
			for (int begin = 0; begin < numTokens - len; begin++) {
				final HashSet<SplitTriplet> triplets = new HashSet<SplitTriplet>();
				spans.put(new SpanPair(begin, begin + len), triplets);
				// Add dummy split for the lexical job
				triplets.add(new SplitTriplet(begin, begin + len, -1));
				for (int split = 0; split < len; split++) {
					triplets.add(new SplitTriplet(begin, begin + len, split));
				}
			}
		}

		// Create a map to hold all completed spans, index them by both
		// start and end points (so most will appear in the map twice).
		final Map<Integer, Set<SpanPair>> completedSpanBegins = new HashMap<Integer, Set<SpanPair>>();
		final Map<Integer, Set<SpanPair>> completedSpanEnds = new HashMap<Integer, Set<SpanPair>>();
		for (int i = 0; i < numTokens; ++i) {
			completedSpanBegins.put(i, new HashSet<SpanPair>());
			completedSpanEnds.put(i, new HashSet<SpanPair>());
		}

		// Lock
		final SpanLock lock = new SpanLock(numTokens);

		// Create the listener
		final Listener listener = new Listener(completedSpanBegins,
				completedSpanEnds, pruningFilter, model, chart, numTokens,
				cellFactory, lock, spans);

		try {
			// Need to sync over splits, in case the jobs will complete before
			// we get to wait on it. Plus, to wait we need to lock it using
			// sync.
			synchronized (spans) {
				// Add all lexical jobs to start the parsing process
				LOG.debug("Creating initial lexical jobs");

				// Create all lexical jobs.
				for (int i = 0; i < numTokens; i++) {
					for (int j = i; j < numTokens; j++) {
						executor.execute(new LexicalJob(cellFactory, chart,
								listener, lock, model, new SpanPair(i, j),
								lexicon, pruningFilter, numTokens));
					}
				}

				// Wait for the set of triplets to be empty, i.e., all spans are
				// processed.
				spans.wait();
			}
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return chart;
	}

	public static class Builder<DI extends Sentence, MR> {

		private final Set<CKYBinaryParsingRule<MR>>				binaryRules					= new HashSet<CKYBinaryParsingRule<MR>>();

		private boolean											breakTies					= false;

		private final ICategoryServices<MR>						categoryServices;

		private IFilter<Category<MR>>							completeParseFilter			= FilterUtils
				.stubTrue();

		private ILexicalRule<MR>								lexicalRule					= new LexicalRule<MR>();

		/** The maximum number of cells allowed in each span */
		private int												maxNumberOfCellsInSpan		= 50;

		private int												numThreads					= Runtime
				.getRuntime().availableProcessors();

		/**
		 * Pre-chart pruning creates a further approximation of the packed chart
		 * which influences non-maximal children. It does mean that worker
		 * threads will take chart span locks for shorter periods. This option
		 * is not to be used for gradient based learning, as it creates
		 * instability in the non-maximal children of a cell.
		 */
		private boolean											preChartPruning				= false;

		private boolean											pruneLexicalCells			= false;

		private final List<ISentenceLexiconGenerator<DI, MR>>	sentenceLexicalGenerators	= new ArrayList<ISentenceLexiconGenerator<DI, MR>>();

		private final List<ISentenceLexiconGenerator<DI, MR>>	sloppyLexicalGenerators		= new ArrayList<ISentenceLexiconGenerator<DI, MR>>();

		private String											threadNamePrefix			= "CKY";

		private final Set<CKYUnaryParsingRule<MR>>				unaryRules					= new HashSet<CKYUnaryParsingRule<MR>>();

		public Builder(ICategoryServices<MR> categoryServices) {
			this.categoryServices = categoryServices;
		}

		public Builder<DI, MR> addParseRule(CKYBinaryParsingRule<MR> rule) {
			binaryRules.add(rule);
			return this;
		}

		public Builder<DI, MR> addParseRule(CKYUnaryParsingRule<MR> rule) {
			unaryRules.add(rule);
			return this;
		}

		public Builder<DI, MR> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<DI, MR> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}

		public Builder<DI, MR> addSloppyLexicalGenerator(
				ISentenceLexiconGenerator<DI, MR> sloppyGenerator) {
			sloppyLexicalGenerators.add(sloppyGenerator);
			return this;
		}

		@SuppressWarnings("unchecked")
		public MultiCKYParser<DI, MR> build() {
			return new MultiCKYParser<DI, MR>(maxNumberOfCellsInSpan,
					binaryRules.toArray((CKYBinaryParsingRule<MR>[]) Array
							.newInstance(CKYBinaryParsingRule.class,
									binaryRules.size())),
					sentenceLexicalGenerators, sloppyLexicalGenerators,
					categoryServices, pruneLexicalCells, preChartPruning,
					completeParseFilter,
					unaryRules.toArray((CKYUnaryParsingRule<MR>[]) Array
							.newInstance(CKYUnaryParsingRule.class,
									unaryRules.size())),
					numThreads, threadNamePrefix, lexicalRule, breakTies);
		}

		public Builder<DI, MR> setBreakTies(boolean breakTies) {
			this.breakTies = breakTies;
			return this;
		}

		public Builder<DI, MR> setCompleteParseFilter(
				IFilter<Category<MR>> completeParseFilter) {
			this.completeParseFilter = completeParseFilter;
			return this;
		}

		public void setLexicalRule(ILexicalRule<MR> lexicalRule) {
			this.lexicalRule = lexicalRule;
		}

		public Builder<DI, MR> setMaxNumberOfCellsInSpan(
				int maxNumberOfCellsInSpan) {
			this.maxNumberOfCellsInSpan = maxNumberOfCellsInSpan;
			return this;
		}

		public Builder<DI, MR> setNumThreads(int numThreads) {
			this.numThreads = numThreads;
			return this;
		}

		public Builder<DI, MR> setPreChartPruning(boolean preChartPruning) {
			if (preChartPruning) {
				LOG.warn(
						"Pre-chart pruning creates instability for gradient-based learners.");
			}
			this.preChartPruning = preChartPruning;
			return this;
		}

		public Builder<DI, MR> setPruneLexicalCells(boolean pruneLexicalCells) {
			this.pruneLexicalCells = pruneLexicalCells;
			return this;
		}

		public Builder<DI, MR> setThreadNamePrefix(String threadNamePrefix) {
			this.threadNamePrefix = threadNamePrefix;
			return this;
		}
	}

	public static class Creator<DI extends Sentence, MR>
			implements IResourceObjectCreator<MultiCKYParser<DI, MR>> {

		private String type;

		public Creator() {
			this("parser.cky.multi");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public MultiCKYParser<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			final Builder<DI, MR> builder = new Builder<DI, MR>(
					(ICategoryServices<MR>) repo.get(
							ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));

			if (params.contains("parseFilter")) {
				builder.setCompleteParseFilter((IFilter<Category<MR>>) repo
						.get(params.get("parseFilter")));
			}

			if (params.contains("beam")) {
				builder.setMaxNumberOfCellsInSpan(params.getAsInteger("beam"));
			}

			if (params.contains("lex")) {
				builder.setLexicalRule(
						(ILexicalRule<MR>) repo.get(params.get("lex")));
			}

			if (params.contains("threads")) {
				builder.setNumThreads(params.getAsInteger("threads"));
			}

			if (params.contains("preChartPruning")) {
				builder.setPreChartPruning(
						params.getAsBoolean("preChartPruning"));
			}

			if (params.contains("breakTies")) {
				builder.setBreakTies(params.getAsBoolean("breakTies"));
			}

			if (params.contains("pruneLexicalCells")) {
				builder.setPruneLexicalCells(
						params.getAsBoolean("pruneLexicalCells"));
			}

			if (params.contains("threadPrefix")) {
				builder.setThreadNamePrefix(params.get("threadsPrefix"));
			}

			for (final String id : params.getSplit("generators")) {
				builder.addSentenceLexicalGenerator(
						(ISentenceLexiconGenerator<DI, MR>) repo.get(id));
			}

			for (final String id : params.getSplit("sloppyGenerators")) {
				builder.addSloppyLexicalGenerator(
						(ISentenceLexiconGenerator<DI, MR>) repo.get(id));
			}

			NormalFormValidator nfValidator;
			if (params.contains("nfValidator")) {
				nfValidator = repo.get(params.get("nfValidator"));
			} else {
				nfValidator = null;
			}

			final String wordSkippingType = params.get("wordSkipping", "none");
			if (wordSkippingType.equals("simple")) {
				// Skipping lexical generator.
				builder.addSloppyLexicalGenerator(
						new SimpleWordSkippingLexicalGenerator<DI, MR>(
								(ICategoryServices<MR>) repo.get(
										ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));

				// Skipping rules.
				final ForwardSkippingRule<MR> forwardSkip = new ForwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo.get(
								ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
				final BackwardSkippingRule<MR> backSkip = new BackwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
						false);

				// Add a normal form constraint to disallow unary steps after
				// skipping.
				final NormalFormValidator.Builder nfBuilder = new NormalFormValidator.Builder();
				if (nfValidator != null) {
					nfBuilder.addConstraints(nfValidator);
				}
				nfBuilder.addConstraint(new UnaryConstraint(SetUtils
						.createSet(forwardSkip.getName(), backSkip.getName())));
				nfValidator = nfBuilder.build();

				// Add the rules.
				addRule(builder, backSkip, nfValidator, params);
				addRule(builder, forwardSkip, nfValidator, params);
			} else if (wordSkippingType.equals("aggressive")) {
				// Skipping lexical generator.
				builder.addSloppyLexicalGenerator(
						new AggressiveWordSkippingLexicalGenerator<DI, MR>(
								(ICategoryServices<MR>) repo.get(
										ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));
				// Skipping rules.
				final ForwardSkippingRule<MR> forwardSkip = new ForwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo.get(
								ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
				final BackwardSkippingRule<MR> backSkip = new BackwardSkippingRule<MR>(
						(ICategoryServices<MR>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
						true);

				// Add a normal form constraint to disallow unary steps after
				// skipping.
				final NormalFormValidator.Builder nfBuilder = new NormalFormValidator.Builder();
				if (nfValidator != null) {
					nfBuilder.addConstraints(nfValidator);
				}
				nfBuilder.addConstraint(new UnaryConstraint(SetUtils
						.createSet(forwardSkip.getName(), backSkip.getName())));
				nfValidator = nfBuilder.build();

				// Add the rules.
				addRule(builder, backSkip, nfValidator, params);
				addRule(builder, forwardSkip, nfValidator, params);
			}

			for (final String id : params.getSplit("rules")) {
				final Object rule = repo.get(id);
				if (rule instanceof BinaryRuleSet) {
					for (final IBinaryParseRule<MR> singleRule : (BinaryRuleSet<MR>) rule) {
						addRule(builder, singleRule, nfValidator, params);
					}
				} else if (rule instanceof UnaryRuleSet) {
					for (final IUnaryParseRule<MR> singleRule : (UnaryRuleSet<MR>) rule) {
						addRule(builder, singleRule, nfValidator, params);
					}
				} else {
					addRule(builder, rule, nfValidator, params);
				}
			}

			return builder.build();
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage.builder(type, MultiCKYParser.class)
					.addParam("breakTies", Boolean.class,
							"Breaks ties during pruning using the order of insertion to the queue. In a multi-threaded parser, this is essentially random (default: false)")
					.addParam("parseFilter", IFilter.class,
							"Filter to determine complete parses.")
					.addParam("beam", Integer.class,
							"Beam to use for cell pruning (default: 50).")
					.addParam("lex", ILexicalRule.class,
							"Lexical rule (default: simple generic rule)")
					.addParam("preChartPruning", Boolean.class,
							"Prune categories before adding to the chart (default: false)")
					.addParam("pruneLexicalCells", Boolean.class,
							"Prune lexical entries similarly to conventional categories (default: false)")
					.addParam("wordSkipping", String.class,
							"Type of word skpping to use during sloppy inference: none, simple or aggressive (default: none)")
					.addParam("generators", ISentenceLexiconGenerator.class,
							"List of dynamic sentence lexical generators.")
					.addParam("sloppyGenerators",
							ISentenceLexiconGenerator.class,
							"List of dynamic sentence lexical generators for sloppy inference.")
					.addParam("transformation", Function.class,
							"Transformation to be applied to each category before it's added to the chart (default: none).")
					.addParam("rules", IBinaryParseRule.class,
							"Binary parsing rules.")
					.build();
		}

		/**
		 * @param params
		 *            The {@link Parameters} are available for use by inheriting
		 *            classes.
		 */
		@SuppressWarnings("unchecked")
		protected void addRule(Builder<DI, MR> builder, Object rule,
				NormalFormValidator nfValidator, Parameters params) {
			if (rule instanceof IBinaryParseRule) {
				builder.addParseRule(new CKYBinaryParsingRule<MR>(
						(IBinaryParseRule<MR>) rule, nfValidator));
			} else if (rule instanceof IUnaryParseRule) {
				builder.addParseRule(new CKYUnaryParsingRule<MR>(
						(IUnaryParseRule<MR>) rule, nfValidator));
			} else if (rule instanceof CKYBinaryParsingRule) {
				builder.addParseRule((CKYBinaryParsingRule<MR>) rule);
			} else if (rule instanceof CKYUnaryParsingRule) {
				builder.addParseRule((CKYUnaryParsingRule<MR>) rule);
			} else {
				throw new IllegalArgumentException(
						"Invalid rule class: " + rule);
			}
		}

	}

	private abstract class AbstractJob extends LoggingRunnable {
		protected final AbstractCellFactory<MR>	cellFactory;
		protected final Chart<MR>				chart;
		protected final Listener				listener;
		protected final SpanLock				lock;
		protected final IDataItemModel<MR>		model;
		protected final int						sentenceLength;
		protected final SplitTriplet			split;

		public AbstractJob(AbstractCellFactory<MR> cellFactory, Chart<MR> chart,
				Listener listener, SpanLock lock, IDataItemModel<MR> model,
				SplitTriplet split, int sentenceLength) {
			this.cellFactory = cellFactory;
			this.chart = chart;
			this.listener = listener;
			this.lock = lock;
			this.model = model;
			this.split = split;
			this.sentenceLength = sentenceLength;
		}
	}

	private class LexicalJob extends AbstractJob {

		private final ILexiconImmutable<MR>		lexicon;
		private final Predicate<ParsingOp<MR>>	pruningFilter;

		public LexicalJob(AbstractCellFactory<MR> cellFactory, Chart<MR> chart,
				Listener listener, SpanLock lock, IDataItemModel<MR> model,
				SpanPair span, ILexiconImmutable<MR> lexicon,
				Predicate<ParsingOp<MR>> pruningFilter, int sentenceLength) {
			super(cellFactory, chart, listener, lock, model,
					new SplitTriplet(span.start, span.end, -1), sentenceLength);
			this.lexicon = lexicon;
			this.pruningFilter = pruningFilter;
			LOG.debug("Created lexical job for %s", split.span);
		}

		@Override
		public void loggedRun() {
			LOG.debug("%s Lexical job started", split.span);

			final Pair<Collection<Cell<MR>>, Boolean> processingPair = generateLexicalCells(
					split.start, split.end, chart, lexicon, model,
					pruningFilter);

			// Add all the valid cells under a span lock.
			lock.lock(split.start, split.end);
			for (final Cell<MR> newCell : processingPair.first()) {
				chart.add(newCell);
			}
			if (processingPair.second()) {
				chart.externalPruning(split.start, split.end);
			}
			lock.unlock(split.start, split.end);

			LOG.debug("%s: Lexical job completed, tried to add %d entries",
					split.span, processingPair.first().size());

			// Signal the job is complete.
			listener.jobComplete(this);
		}
	}

	private class Listener {
		private final IndexLock							adjacentLock;
		private final AbstractCellFactory<MR>			cellFactory;
		private final Chart<MR>							chart;
		private final Map<Integer, Set<SpanPair>>		completedSpanBegins;
		private final Map<Integer, Set<SpanPair>>		completedSpanEnds;
		private final SpanLock							lock;
		private final IDataItemModel<MR>				model;
		private final int								numTokens;
		private final Predicate<ParsingOp<MR>>			pruningFilter;
		private final Map<SpanPair, Set<SplitTriplet>>	spans;

		public Listener(Map<Integer, Set<SpanPair>> completedSpanBegins,
				Map<Integer, Set<SpanPair>> completedSpanEnds,
				Predicate<ParsingOp<MR>> pruningFilter,
				IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
				AbstractCellFactory<MR> cellFactory, SpanLock lock,
				Map<SpanPair, Set<SplitTriplet>> spans) {
			this.completedSpanBegins = completedSpanBegins;
			this.completedSpanEnds = completedSpanEnds;
			this.pruningFilter = pruningFilter;
			this.model = model;
			this.chart = chart;
			this.numTokens = numTokens;
			this.cellFactory = cellFactory;
			this.lock = lock;
			this.spans = spans;
			this.adjacentLock = new IndexLock(numTokens);
		}

		public void jobComplete(AbstractJob job) {
			final boolean doSpanUnary;
			final boolean spanComplete;

			synchronized (spans) {
				// Remove split from splits set
				final Set<SplitTriplet> spanSplits = spans.get(job.split.span);
				final boolean removed = spanSplits.remove(job.split);

				// Case all splits for this span are processed
				if (removed && spanSplits.isEmpty()) {
					// Case last split in the span finished. Next, should
					// process the span with unary rules.
					spanComplete = false;
					doSpanUnary = true;
				} else if (spanSplits.isEmpty()) {
					// Case the span was empty of splits, so this is the unary
					// job returning. Mark that we complete the span.
					spanComplete = true;
					doSpanUnary = false;
				} else {
					// Case split jobs are still pending.
					spanComplete = false;
					doSpanUnary = false;
				}

				// If the span is complete, can remove it from the spans map.
				if (spanComplete) {
					LOG.debug("Span complete: %s", job.split.span);

					// Remove span
					spans.remove(job.split.span);

					if (spans.isEmpty()) {
						// Case all splits processed, notify the spans object.
						// The main thread is waiting on this object.
						LOG.debug("All spans complete -- notifying parser");
						spans.notifyAll();
					}
				}
			}

			if (doSpanUnary) {
				// Case all splits processed, we still require processing with
				// unary rules.
				executor.execute(new UnarySpanJob(pruningFilter, model, chart,
						cellFactory, job.split.span, lock, this, numTokens));
			}

			// If span is complete, process it with unary rules.
			if (spanComplete) {
				// Case the span is complete, including processing with unary
				// rules.
				LOG.debug("Span completed: %s", job.split.span);

				// Iterate over all neighboring completed spans, and
				// create and queue split jobs with them. For each
				// pair of spans only one of them gets here while the other
				// already exists, so there's no duplicate work. This is why
				// it's critical to access this part under the same lock in
				// which the newly added span is added to the being and ends
				// maps.

				// Block I: Queue jobs with spans on the left

				// Lock to stop all spans that are adjacent to the current
				// span on the left from getting into block II.
				if (job.split.start > 0) {
					adjacentLock.lock(job.split.start - 1);
					for (final SpanPair leftSpan : completedSpanEnds
							.get(job.split.start - 1)) {
						executor.execute(new SplitJob(pruningFilter, model,
								chart, cellFactory,
								new SplitTriplet(leftSpan.start, job.split.end,
										leftSpan.end - leftSpan.start),
								lock, this, numTokens));
					}
				}
				// Add the beginning index of the span to the completed
				// map.
				completedSpanBegins.get(job.split.start).add(job.split.span);
				if (job.split.start > 0) {
					adjacentLock.unlock(job.split.start - 1);
				}

				// Block II: Queue jobs with spans on the right.

				// Lock to stop all spans that are adjacent to the current
				// span on the right from getting into block I.
				if (job.split.end < numTokens - 1) {
					adjacentLock.lock(job.split.end);
					for (final SpanPair rightSpan : completedSpanBegins
							.get(job.split.end + 1)) {
						executor.execute(new SplitJob(pruningFilter, model,
								chart, cellFactory,
								new SplitTriplet(job.split.start, rightSpan.end,
										job.split.end - job.split.start),
								lock, this, numTokens));
					}
				}
				// Add the end index of the span to the completed map
				completedSpanEnds.get(job.split.end).add(job.split.span);
				if (job.split.end < numTokens - 1) {
					adjacentLock.unlock(job.split.end);
				}
			}
		}
	}

	private static class SpanPair {
		final int	end;
		final int	hashCode;
		final int	start;

		public SpanPair(int begin, int end) {
			this.start = begin;
			this.end = end;
			this.hashCode = calcHashCode();
		}

		public int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + start;
			result = prime * result + end;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SpanPair other = (SpanPair) obj;
			if (start != other.start) {
				return false;
			}
			if (end != other.end) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return "(" + start + "-" + end + ")";
		}
	}

	private class SplitJob extends AbstractJob {

		private final Predicate<ParsingOp<MR>> pruningFilter;

		public SplitJob(Predicate<ParsingOp<MR>> pruningFilter,
				IDataItemModel<MR> model, Chart<MR> chart,
				AbstractCellFactory<MR> cellFactory, SplitTriplet split,
				SpanLock lock, Listener listener, int sentenceLength) {
			super(cellFactory, chart, listener, lock, model, split,
					sentenceLength);
			this.pruningFilter = pruningFilter;
			LOG.debug("Created split job for %s", split);
		}

		@Override
		public void loggedRun() {
			LOG.debug("%s: Split job started", split);

			final Pair<List<Cell<MR>>, Boolean> processingPair = preChartPruning
					? processSplitAndPrune(split.start, split.end, split.split,
							sentenceLength, chart, cellFactory, pruningFilter,
							chart.getBeamSize(), model)
					: processSplit(split.start, split.end, split.split,
							sentenceLength, chart, cellFactory, pruningFilter,
							model);

			final List<Cell<MR>> newCells = processingPair.first();

			LOG.debug("%s: %d new cells", split, newCells.size());

			// Add all the valid cells under a span lock
			lock.lock(split.start, split.end);
			for (final Cell<MR> newCell : newCells) {
				chart.add(newCell);
			}
			if (processingPair.second()) {
				chart.externalPruning(split.start, split.end);
			}
			lock.unlock(split.start, split.end);

			LOG.debug("%s: Split job completed", split);

			// Signal the job is complete
			listener.jobComplete(this);
		}
	}

	private static class SplitTriplet {
		final int		end;
		final int		hashCode;
		final SpanPair	span;
		final int		split;
		final int		start;

		public SplitTriplet(int begin, int end, int split) {
			this.start = begin;
			this.end = end;
			this.split = split;
			this.span = new SpanPair(begin, end);
			this.hashCode = calcHashCode();
		}

		public int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + start;
			result = prime * result + end;
			result = prime * result + split;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SplitTriplet other = (SplitTriplet) obj;
			if (start != other.start) {
				return false;
			}
			if (end != other.end) {
				return false;
			}
			if (split != other.split) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			if (split < 0) {
				return "(" + start + "-" + end + ")";
			} else {
				return "(" + start + "-" + (start + split) + ", "
						+ (start + split + 1) + "-" + end + ")";
			}
		}

	}

	/**
	 * Process a single span using all unary rules. Assumes the span has been
	 * processed using {@link LexicalJob} and {@link SplitJob}.
	 *
	 * @author Yoav Artzi
	 */
	private class UnarySpanJob extends AbstractJob {

		private final Predicate<ParsingOp<MR>> pruningFilter;

		public UnarySpanJob(Predicate<ParsingOp<MR>> pruningFilter,
				IDataItemModel<MR> model, Chart<MR> chart,
				AbstractCellFactory<MR> cellFactory, SpanPair span,
				SpanLock lock, Listener listener, int sentenceLength) {
			super(cellFactory, chart, listener, lock, model,
					new SplitTriplet(span.start, span.end, -1), sentenceLength);
			this.pruningFilter = pruningFilter;
			LOG.debug("Created unary job for %s", split.span);
		}

		@Override
		public void loggedRun() {
			LOG.debug("%s: Unary span job started", split.span);

			final Pair<List<Cell<MR>>, Boolean> processingPair = preChartPruning
					? unaryProcessSpanAndPrune(split.start, split.end,
							sentenceLength, chart, cellFactory, pruningFilter,
							chart.getBeamSize(), model)
					: unaryProcessSpan(split.start, split.end, sentenceLength,
							chart, cellFactory, pruningFilter, model);

			final List<Cell<MR>> newCells = processingPair.first();

			LOG.debug("%s: %d new cells", split, newCells.size());

			// Add all the valid cells under a span lock.
			lock.lock(split.start, split.end);
			for (final Cell<MR> newCell : newCells) {
				chart.add(newCell);
			}
			if (processingPair.second()) {
				chart.externalPruning(split.start, split.end);
			}
			lock.unlock(split.start, split.end);

			LOG.debug("%s: Unary span job completed", split);

			// Signal the job is complete
			listener.jobComplete(this);
		}
	}

}
