/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi
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

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.data.IDataCollection;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.IUBLSplitter;
import edu.uw.cs.lil.tiny.learn.ubl.splitting.Splitter;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.test.Tester;

/**
 * Factory for {@link UBLPerceptron}.
 * 
 * @author Yoav Artzi
 */
public class UBLPerceptronBuilder {
	
	/**
	 * These are used to define the temperature of parameter updates. temp =
	 * alpha_0/(1+c*tot_number_of_training_instances)
	 */
	private double																			alpha0			= 0.1;
	
	/**
	 * These are used to define the temperature of parameter updates. temp =
	 * alpha_0/(1+c*tot_number_of_training_instances)
	 */
	private double																			c				= 0.0001;
	
	private final ICategoryServices<LogicalExpression>										categoryServices;
	
	/**
	 * Number of training epochs (rounds).
	 */
	private int																				epochs			= 10;
	
	/**
	 * Expand the lexicon during learning using higher order unification.
	 */
	private boolean																			expandLexicon	= false;
	
	/**
	 * Maximum sentence length to process. Skip any sentence that is longer.
	 */
	private int																				maxSentLen		= 50;
	
	private final AbstractCKYParser<LogicalExpression>										parser;
	
	private IUBLSplitter																	splitter;
	
	private Tester<Sentence, LogicalExpression>												tester;
	
	private final IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>>	trainData;
	
	public UBLPerceptronBuilder(
			IDataCollection<? extends ILabeledDataItem<Sentence, LogicalExpression>> trainData,
			ICategoryServices<LogicalExpression> categoryServices,
			AbstractCKYParser<LogicalExpression> parser) {
		this.categoryServices = categoryServices;
		this.trainData = trainData;
		this.parser = parser;
		this.splitter = new Splitter(categoryServices);
	}
	
	public UBLPerceptron build() {
		return new UBLPerceptron(tester, trainData, categoryServices,
				expandLexicon, alpha0, c, epochs, maxSentLen, splitter, parser);
	}
	
	public UBLPerceptronBuilder setAlpha0(double alpha0) {
		this.alpha0 = alpha0;
		return this;
	}
	
	public UBLPerceptronBuilder setC(double c) {
		this.c = c;
		return this;
	}
	
	public UBLPerceptronBuilder setEpochs(int epochs) {
		this.epochs = epochs;
		return this;
	}
	
	public UBLPerceptronBuilder setExpandLexicon(boolean expandLexicon) {
		this.expandLexicon = expandLexicon;
		return this;
	}
	
	public UBLPerceptronBuilder setMaxSentLen(int maxSentLen) {
		this.maxSentLen = maxSentLen;
		return this;
	}
	
	public UBLPerceptronBuilder setSplitter(IUBLSplitter splitter) {
		this.splitter = splitter;
		return this;
	}
	
	public UBLPerceptronBuilder setTester(
			Tester<Sentence, LogicalExpression> tester) {
		this.tester = tester;
		return this;
	}
}
