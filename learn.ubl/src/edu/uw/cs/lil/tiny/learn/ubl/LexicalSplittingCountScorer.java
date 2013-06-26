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
package edu.uw.cs.lil.tiny.learn.ubl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.Lexeme;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.LexicalTemplate;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.AbstractDecoderIntoFile;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.parser.ccg.model.storage.IDecoder;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.counter.Counter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Returns a score for a LexicalEntry<Y> that is based on co-occurance
 * statistics computed over a set of lexical entries. The statistics are
 * computed by counting the number of times that (1) a phrase occurs in one of
 * the input lexical entries and (2) a category can be found by splitting one of
 * the input categories recursively, up to n times. This class includes the code
 * for computing the statistics, storing them, and evaluating new lexical items.
 * 
 * @author Luke Zettlemoyer
 */

public class LexicalSplittingCountScorer implements IScorer<Lexeme> {
	private static final ILogger						LOG	= LoggerFactory
																	.create(LexicalSplittingCountScorer.class);
	
	private final Map<List<LogicalConstant>, Counter>	catCounts;
	private final Map<Lexeme, Counter>					lexCounts;
	private final int									numCounted;
	private final Map<List<String>, Counter>			wordCounts;
	
	private LexicalSplittingCountScorer(
			Map<List<LogicalConstant>, Counter> catCounts,
			Map<Lexeme, Counter> lexCounts,
			Map<List<String>, Counter> wordCounts, int numCounted) {
		this.catCounts = catCounts;
		this.lexCounts = lexCounts;
		this.wordCounts = wordCounts;
		this.numCounted = numCounted;
	}
	
	public static IDecoder<LexicalSplittingCountScorer> getDecoder(
			DecoderHelper<LogicalExpression> decoderHelper) {
		return new Decoder(decoderHelper);
	}
	
	private static Set<List<String>> allSubStrings(int maxLength,
			List<String> tokens) {
		final Set<List<String>> result = new HashSet<List<String>>();
		for (int start = 0; start < tokens.size(); start++) {
			for (int length = 1; start + length <= tokens.size()
					&& length <= maxLength; length++) {
				result.add(tokens.subList(start, start + length));
			}
		}
		return result;
	}
	
	/**
	 * Make the set of all categories that can be constructed by splitting the
	 * input entry @param depth times (and any new ones that are created in the
	 * process)
	 */
	private static Set<Category<LogicalExpression>> recursiveCategorySplits(
			int depth, Category<LogicalExpression> category,
			ICategoryServices<LogicalExpression> categoryServices,
			IUBLSplitter splitter) {
		
		if (depth < 1) {
			return Collections.emptySet();
		}
		
		final Set<Category<LogicalExpression>> newCategories = new HashSet<Category<LogicalExpression>>();
		final Set<Category<LogicalExpression>> subCategories = new HashSet<Category<LogicalExpression>>();
		
		// first split the category, by reversing either application or
		// composition
		final Set<SplittingPair> categorySplits = splitter.getSplits(category);
		
		for (final SplittingPair split : categorySplits) {
			final Category<LogicalExpression> leftSplit = split.getLeft();
			final Category<LogicalExpression> rightSplit = split.getRight();
			
			if (!newCategories.contains(leftSplit)) {
				newCategories.add(leftSplit);
				if (depth > 1) {
					subCategories.addAll(recursiveCategorySplits(depth - 1,
							leftSplit, categoryServices, splitter));
				}
			}
			
			if (!newCategories.contains(rightSplit)) {
				newCategories.add(rightSplit);
				if (depth > 1) {
					subCategories.addAll(recursiveCategorySplits(depth - 1,
							rightSplit, categoryServices, splitter));
				}
			}
		}
		newCategories.addAll(subCategories);
		return newCategories;
	}
	
	public void printCounts() {
		final List<Map.Entry<Lexeme, Counter>> sorted = new ArrayList<Map.Entry<Lexeme, Counter>>(
				lexCounts.entrySet());
		
		Collections.sort(sorted, new Comparator<Map.Entry<Lexeme, Counter>>() {
			public int compare(Map.Entry<Lexeme, Counter> o1,
					Map.Entry<Lexeme, Counter> o2) {
				return o1.getValue().value() - o2.getValue().value();
			}
		});
		
		for (final Map.Entry<Lexeme, Counter> entry : sorted) {
			final Lexeme lex = entry.getKey();
			
			LOG.info("%s\t%s\t%s\t%s\t%s", entry.getValue(),
					wordCounts.get(lex.getTokens()),
					catCounts.get(lex.getConstants()), score(lex), lex);
		}
	}
	
	@Override
	public double score(Lexeme lexeme) {
		
		final Counter lexemeCount = lexCounts.get(lexeme);
		if (lexemeCount == null) {
			return 0.0;
		}
		final double lexGivenWordProb = lexemeCount.value()
				/ (double) wordCounts.get(lexeme.getTokens()).value();
		final double lexGivenCatProb = lexemeCount.value()
				/ (double) catCounts.get(lexeme.getConstants()).value();
		
		final double score = ((lexemeCount.value() / (double) numCounted) * (lexGivenWordProb + lexGivenCatProb));
		return score;
	}
	
	public static class Builder {
		private final Map<List<LogicalConstant>, Counter>	catCounts	= new HashMap<List<LogicalConstant>, Counter>();
		private final ICategoryServices<LogicalExpression>	categoryServices;
		private final Set<LexicalEntry<LogicalExpression>>	entries;
		private final Map<Lexeme, Counter>					lexCounts	= new HashMap<Lexeme, Counter>();
		
		private int											numCounted	= 0;
		private final IUBLSplitter							splitter;
		private final Map<List<String>, Counter>			wordCounts	= new HashMap<List<String>, Counter>();
		
		public Builder(
				IUBLSplitter splitter,
				IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> data,
				ICategoryServices<LogicalExpression> categoryServices) {
			this.splitter = splitter;
			this.entries = UBLServices.createSentenceLexicalEntries(data,
					categoryServices);
			this.categoryServices = categoryServices;
		}
		
		public LexicalSplittingCountScorer build() {
			// loop through all entries in the input set
			for (final LexicalEntry<LogicalExpression> entry : entries) {
				
				final Set<Category<LogicalExpression>> allSplits = recursiveCategorySplits(
						2, entry.getCategory(), categoryServices, splitter);
				
				final Set<List<String>> subStrings = allSubStrings(4,
						entry.getTokens());
				
				// now, do the counting for each possible word span
				count(allSplits, subStrings);
			}
			
			return new LexicalSplittingCountScorer(catCounts, lexCounts,
					wordCounts, numCounted);
			
		}
		
		private void count(Set<Category<LogicalExpression>> categories,
				Set<List<String>> strings) {
			
			numCounted++;
			
			final Set<List<LogicalConstant>> constants = new HashSet<List<LogicalConstant>>();
			final Set<Lexeme> lexemes = new HashSet<Lexeme>();
			for (final Category<LogicalExpression> cat : categories) {
				final List<LogicalConstant> cons = LexicalTemplate.doFactoring(
						cat, AbstractUBL.SPLITTING_LEXICAL_ORIGIN).first();
				constants.add(cons);
				for (final List<String> tokens : strings) {
					final Lexeme lexeme = new Lexeme(tokens, cons,
							AbstractUBL.SPLITTING_LEXICAL_ORIGIN);
					if (!lexemes.contains(lexeme)) {
						// only count them once...
						lexemes.add(lexeme);
						if (lexCounts.containsKey(lexeme)) {
							lexCounts.get(lexeme).inc();
						} else {
							lexCounts.put(lexeme, new Counter(1));
						}
					}
				}
			}
			
			for (final List<LogicalConstant> cat : constants) {
				if (catCounts.containsKey(cat)) {
					catCounts.get(cat).inc();
				} else {
					catCounts.put(cat, new Counter(1));
				}
			}
			for (final List<String> tokens : strings) {
				if (wordCounts.containsKey(tokens)) {
					wordCounts.get(tokens).inc();
				} else {
					wordCounts.put(tokens, new Counter(1));
				}
			}
		}
		
	}
	
	private static class Decoder extends
			AbstractDecoderIntoFile<LexicalSplittingCountScorer> {
		private static final int						VERSION	= 1;
		private final DecoderHelper<LogicalExpression>	decoderHelper;
		
		public Decoder(DecoderHelper<LogicalExpression> decoderHelper) {
			super(LexicalSplittingCountScorer.class);
			this.decoderHelper = decoderHelper;
		}
		
		@Override
		public int getVersion() {
			return VERSION;
		}
		
		@Override
		protected Map<String, String> createAttributesMap(
				LexicalSplittingCountScorer object) {
			final Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("numCounted", Integer.toString(object.numCounted));
			return attributes;
		}
		
		@Override
		protected LexicalSplittingCountScorer doDecode(
				Map<String, String> attributes,
				Map<String, File> dependentFiles, BufferedReader reader)
				throws IOException {
			final int numCounted = Integer
					.valueOf(attributes.get("numCounted"));
			
			String line;
			
			// Read category counts
			final Map<List<LogicalConstant>, Counter> catCounts = new HashMap<List<LogicalConstant>, Counter>();
			readTextLine(reader);
			while (!(line = readTextLine(reader)).equals("CATEGORY_COUNTS_END")) {
				final String[] split = line.split("\t");
				final List<LogicalConstant> consts = ListUtils.map(Arrays
						.asList(Arrays.copyOfRange(split, 1, split.length)),
						new ListUtils.Mapper<String, LogicalConstant>() {
							@Override
							public LogicalConstant process(String obj) {
								return (LogicalConstant) decoderHelper
										.getCategoryServices().parseSemantics(
												obj);
							}
						});
				catCounts.put(consts, new Counter(Integer.valueOf(split[0])));
			}
			
			// Read lexeme counts
			final Map<Lexeme, Counter> lexCounts = new HashMap<Lexeme, Counter>();
			readTextLine(reader);
			while (!(line = readTextLine(reader)).equals("LEXEME_COUNTS_END")) {
				final String[] split = line.split("\t", 2);
				lexCounts.put(Lexeme.parse(split[1],
						decoderHelper.getCategoryServices(),
						Lexicon.SAVED_LEXICON_ORIGIN),
						new Counter(Integer.valueOf(split[0])));
			}
			
			// Read word counts
			final Map<List<String>, Counter> wordCounts = new HashMap<List<String>, Counter>();
			readTextLine(reader);
			while (!(line = readTextLine(reader)).equals("WORD_COUNTS_END")) {
				final String[] split = line.split("\t");
				final List<String> words = Arrays.asList(Arrays.copyOfRange(
						split, 1, split.length));
				wordCounts.put(words, new Counter(Integer.valueOf(split[0])));
			}
			
			return new LexicalSplittingCountScorer(catCounts, lexCounts,
					wordCounts, numCounted);
		}
		
		@Override
		protected void doEncode(LexicalSplittingCountScorer object,
				BufferedWriter writer) throws IOException {
			// Write category counts
			writer.write("CATEGORY_COUNTS_START\n");
			for (final Map.Entry<List<LogicalConstant>, Counter> entry : object.catCounts
					.entrySet()) {
				writer.write(Integer.toString(entry.getValue().value()));
				writer.write('\t');
				final Iterator<LogicalConstant> iterator = entry.getKey()
						.iterator();
				while (iterator.hasNext()) {
					writer.write(iterator.next().toString());
					if (iterator.hasNext()) {
						writer.write('\t');
					}
				}
				writer.write('\n');
			}
			writer.write("CATEGORY_COUNTS_END\n");
			
			// Write lexeme counts
			writer.write("LEXEME_COUNTS_START\n");
			for (final Map.Entry<Lexeme, Counter> entry : object.lexCounts
					.entrySet()) {
				writer.write(Integer.toString(entry.getValue().value()));
				writer.write('\t');
				writer.write(entry.getKey().toString());
				writer.write('\n');
			}
			writer.write("LEXEME_COUNTS_END\n");
			
			// Write word counts
			writer.write("WORD_COUNTS_START\n");
			for (final Entry<List<String>, Counter> entry : object.wordCounts
					.entrySet()) {
				writer.write(Integer.toString(entry.getValue().value()));
				writer.write('\t');
				final Iterator<String> iterator = entry.getKey().iterator();
				while (iterator.hasNext()) {
					writer.write(iterator.next());
					if (iterator.hasNext()) {
						writer.write('\t');
					}
				}
				writer.write('\n');
			}
			writer.write("WORD_COUNTS_END\n");
		}
		
		@Override
		protected Map<String, File> encodeDependentFiles(
				LexicalSplittingCountScorer object, File directory,
				File parentFile) throws IOException {
			return new HashMap<String, File>();
		}
	}
}
