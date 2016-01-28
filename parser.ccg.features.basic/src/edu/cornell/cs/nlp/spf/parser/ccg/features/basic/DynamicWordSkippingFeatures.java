package edu.cornell.cs.nlp.spf.parser.ccg.features.basic;

import java.util.Collections;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.lexical.ILexicalFeatureSet;

/**
 * Features to account for word skipping cost. Usually, these features are never
 * seen during learning. Therefore, in practice, they are a fixed cost. The
 * features in this set are only triggered for dynamic lexical entries. See
 * entries with an EMPTY category will are not featurized by this set.
 *
 * @author Yoav Artzi
 */
public class DynamicWordSkippingFeatures<DI extends IDataItem<?>, MR>
		implements ILexicalFeatureSet<DI, MR> {
	private static final String	DEFAULT_FEATURE_TAG	= "DYNSKIP";
	private static final long	serialVersionUID	= 947396425069429189L;
	private final Category<MR>	emptyCategory;
	private final String		featureTag;

	public DynamicWordSkippingFeatures(Category<MR> emptyCategory) {
		this(DEFAULT_FEATURE_TAG, emptyCategory);
	}

	public DynamicWordSkippingFeatures(String featureTag,
			Category<MR> emptyCategory) {
		this.featureTag = featureTag;
		this.emptyCategory = emptyCategory;
	}

	@Override
	public boolean addEntry(LexicalEntry<MR> entry,
			IHashVector parametersVector) {
		// Nothing to do.
		return false;
	}

	@Override
	public Set<KeyArgs> getDefaultFeatures() {
		return Collections.emptySet();
	}

	@Override
	public void setFeatures(IParseStep<MR> parseStep, IHashVector features,
			DI dataItem) {
		if (parseStep instanceof ILexicalParseStep) {
			final LexicalEntry<MR> lexicalEntry = ((ILexicalParseStep<MR>) parseStep)
					.getLexicalEntry();
			if (lexicalEntry.isDynamic()
					&& emptyCategory.equals(lexicalEntry.getCategory())) {
				features.add(featureTag, 1.0 * lexicalEntry.getTokens().size());
			}
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<DynamicWordSkippingFeatures<DI, MR>> {

		private final String type;

		public Creator() {
			this("feat.lex.dynskip");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public DynamicWordSkippingFeatures<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			return new DynamicWordSkippingFeatures<>(
					params.get("tag", DEFAULT_FEATURE_TAG),
					((ICategoryServices<MR>) repo
							.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE))
									.getEmptyCategory());
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, DynamicWordSkippingFeatures.class)
					.setDescription(
							"Features to account for word skipping cost. Usually, these features are never seen during learning. Therefore, in practice, they are a fixed cost. The features in this set are only triggered for dynamic lexical entries. See entries with an EMPTY category will are not featurized by this set.")
					.addParam("tag", String.class, "Feature tag (default: "
							+ DEFAULT_FEATURE_TAG + ")")
					.build();
		}

	}

}
