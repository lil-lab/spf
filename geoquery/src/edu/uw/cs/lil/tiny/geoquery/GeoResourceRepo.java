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
package edu.uw.cs.lil.tiny.geoquery;

import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.data.collection.CompositeDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.sentence.SentenceLengthFilter;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentenceDataset;
import edu.uw.cs.lil.tiny.data.utils.LabeledValidator;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.lil.tiny.genlex.ccg.template.TemplateSupervisedGenlex;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.UnificationGenlex;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.UnificationModelInit;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.Splitter;
import edu.uw.cs.lil.tiny.learn.validation.perceptron.ValidationPerceptron;
import edu.uw.cs.lil.tiny.learn.validation.stocgrad.ValidationStocGrad;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.SimpleFullParseFilter;
import edu.uw.cs.lil.tiny.parser.ccg.cky.multi.MultiCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.LexemeFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.LexicalTemplateFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.features.scorers.LexemeCooccurrenceScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.LexicalFeaturesInit;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.RuleUsageFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.ExpLengthLexicalEntryScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.SkippingSensitiveLexicalEntryScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.scorer.UniformScorer;
import edu.uw.cs.lil.tiny.parser.ccg.features.lambda.LogicalExpressionCoordinationFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.LexiconModelInit;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.model.ModelLogger;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.ThatlessRelative;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.ApplicationCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition.CompositionCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.SkippingRuleCreator;
import edu.uw.cs.lil.tiny.test.Tester;

public class GeoResourceRepo extends ResourceCreatorRepository {
	public GeoResourceRepo() {
		// Parser creators
		registerResourceCreator(new ApplicationCreator<LogicalExpression>());
		registerResourceCreator(new CompositionCreator<LogicalExpression>());
		registerResourceCreator(new PrepositionTypeShifting.Creator());
		registerResourceCreator(new SkippingRuleCreator<LogicalExpression>());
		registerResourceCreator(new ForwardTypeRaisedComposition.Creator());
		registerResourceCreator(new ThatlessRelative.Creator());
		registerResourceCreator(new PluralExistentialTypeShifting.Creator());
		registerResourceCreator(new MultiCKYParser.Creator<LogicalExpression>());
		registerResourceCreator(new SimpleFullParseFilter.Creator());
		
		registerResourceCreator(new ExpLengthLexicalEntryScorer.Creator<LogicalExpression>());
		registerResourceCreator(new LexicalFeaturesInit.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new SingleSentenceDataset.Creator());
		registerResourceCreator(new CompositeDataCollection.Creator<SingleSentence>());
		registerResourceCreator(new Model.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new ModelLogger.Creator());
		registerResourceCreator(new LexicalTemplateFeatureSet.Creator<Sentence>());
		registerResourceCreator(new LexicalFeatureSet.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new LexemeFeatureSet.Creator<Sentence>());
		registerResourceCreator(new UniformScorer.Creator<LogicalExpression>());
		registerResourceCreator(new SkippingSensitiveLexicalEntryScorer.Creator<LogicalExpression>());
		registerResourceCreator(new LogicalExpressionCoordinationFeatureSet.Creator<Sentence>());
		registerResourceCreator(new FactoredLexicon.Creator());
		registerResourceCreator(new SingleSentenceDataset.Creator());
		registerResourceCreator(new TemplateSupervisedGenlex.Creator<SingleSentence>());
		registerResourceCreator(new SingleSentenceDataset.Creator());
		registerResourceCreator(new ValidationPerceptron.Creator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(new ValidationStocGrad.Creator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(new LabeledValidator.Creator<SingleSentence, LogicalExpression>());
		registerResourceCreator(new Tester.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new LexiconModelInit.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new UnificationGenlex.Creator<SingleSentence>());
		registerResourceCreator(new Splitter.Creator());
		registerResourceCreator(new UnificationModelInit.Creator());
		registerResourceCreator(new LexemeCooccurrenceScorer.Creator());
		registerResourceCreator(new SentenceLengthFilter.Creator<SingleSentence>());
		registerResourceCreator(new RuleUsageFeatureSet.Creator<Sentence, LogicalExpression>());
	}
}
