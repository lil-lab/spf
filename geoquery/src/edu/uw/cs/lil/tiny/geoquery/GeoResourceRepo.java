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

import edu.uw.cs.lil.tiny.data.resources.CompositeDataCollectionCreator;
import edu.uw.cs.lil.tiny.data.resources.LabeledValidatorCreator;
import edu.uw.cs.lil.tiny.data.resources.SentenceLengthFilterCreator;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.singlesentence.SingleSentence;
import edu.uw.cs.lil.tiny.data.singlesentence.resources.SingleSentenceDatasetCreator;
import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.lil.tiny.genlex.ccg.template.resources.TemplateSupervisedGenlexCreator;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.resources.SplitterCreator;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.resources.UnificationGenlexCreator;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.resources.UnificationModelInitCreator;
import edu.uw.cs.lil.tiny.learn.validation.resources.ValidationPerceptronCreator;
import edu.uw.cs.lil.tiny.learn.validation.resources.ValidationStocGradCreator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.SimpleFullParseFilter;
import edu.uw.cs.lil.tiny.parser.ccg.cky.multi.MultiCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.resources.FactoredLexiconCreator;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.resources.LexemeCooccurrenceScorerCreator;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.resources.LexemeFeatureSetCreator;
import edu.uw.cs.lil.tiny.parser.ccg.factoredlex.resources.LexicalTemplateFeatureSetCreator;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.resources.LexicalFeatureSetCreator;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.resources.SkippingSensitiveLexicalEntryScorerCreator;
import edu.uw.cs.lil.tiny.parser.ccg.features.basic.resources.UniformScorerCreator;
import edu.uw.cs.lil.tiny.parser.ccg.features.lambda.resources.LogicalExpressionCoordinationFeatureSetCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.OverloadedRulesCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.basic.PrepositionTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.templated.ForwardTypeRaisedComposition;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.templated.PluralExistentialTypeShifting;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.templated.ThatlessRelative;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.ApplicationCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.CompositionCreator;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.SkippingRuleCreator;
import edu.uw.cs.lil.tiny.parser.resources.LexiconModelInitCreator;
import edu.uw.cs.lil.tiny.parser.resources.ModelCreator;
import edu.uw.cs.lil.tiny.parser.resources.ModelLoggerCreator;
import edu.uw.cs.lil.tiny.test.resources.TesterCreator;

public class GeoResourceRepo extends ResourceCreatorRepository {
	public GeoResourceRepo() {
		// Parser creators
		registerResourceCreator(new OverloadedRulesCreator<LogicalExpression>());
		registerResourceCreator(new ApplicationCreator<LogicalExpression>());
		registerResourceCreator(new CompositionCreator<LogicalExpression>());
		registerResourceCreator(new PrepositionTypeShifting.Creator());
		registerResourceCreator(new SkippingRuleCreator<LogicalExpression>());
		registerResourceCreator(new ForwardTypeRaisedComposition.Creator());
		registerResourceCreator(new ThatlessRelative.Creator());
		registerResourceCreator(new PluralExistentialTypeShifting.Creator());
		registerResourceCreator(new MultiCKYParser.Creator<LogicalExpression>());
		registerResourceCreator(new SimpleFullParseFilter.Creator());
		
		registerResourceCreator(new SingleSentenceDatasetCreator());
		registerResourceCreator(new CompositeDataCollectionCreator<SingleSentence>());
		registerResourceCreator(new ModelCreator<Sentence, LogicalExpression>());
		registerResourceCreator(new ModelLoggerCreator());
		registerResourceCreator(new LexicalTemplateFeatureSetCreator<Sentence>());
		registerResourceCreator(new LexicalFeatureSetCreator<Sentence, LogicalExpression>());
		registerResourceCreator(new LexemeFeatureSetCreator<Sentence>());
		registerResourceCreator(new UniformScorerCreator<LogicalExpression>());
		registerResourceCreator(new SkippingSensitiveLexicalEntryScorerCreator<LogicalExpression>());
		registerResourceCreator(new LogicalExpressionCoordinationFeatureSetCreator<Sentence>());
		registerResourceCreator(new FactoredLexiconCreator());
		registerResourceCreator(new SingleSentenceDatasetCreator());
		registerResourceCreator(new TemplateSupervisedGenlexCreator());
		registerResourceCreator(new SingleSentenceDatasetCreator());
		registerResourceCreator(new ValidationPerceptronCreator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(new ValidationStocGradCreator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(new LabeledValidatorCreator<SingleSentence, LogicalExpression>());
		registerResourceCreator(new TesterCreator<Sentence, LogicalExpression>());
		registerResourceCreator(new LexiconModelInitCreator<Sentence, LogicalExpression>());
		registerResourceCreator(new UnificationGenlexCreator());
		registerResourceCreator(new SplitterCreator());
		registerResourceCreator(new UnificationModelInitCreator());
		registerResourceCreator(new LexemeCooccurrenceScorerCreator());
		registerResourceCreator(new SentenceLengthFilterCreator());
	}
}
