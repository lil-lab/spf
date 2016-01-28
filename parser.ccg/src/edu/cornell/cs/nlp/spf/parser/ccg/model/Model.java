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
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVectorImmutable;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.model.lexical.ILexicalFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSetImmutable;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * A complete parsing model, including features, parameters and a lexicon.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item used for inference.
 * @param <MR>
 *            Type of semantics (output).
 */
public class Model<DI extends IDataItem<?>, MR>
		implements IModelImmutable<DI, MR> {
	public static final ILogger									LOG					= LoggerFactory
			.create(Model.class);

	private static final long									serialVersionUID	= -1858505634505111170L;

	private final List<IParseFeatureSet<DI, MR>>				featureSets;

	private final List<IIndependentLexicalFeatureSet<DI, MR>>	independentLexicalFeatureSets;

	private final Set<KeyArgs>									invalidFeatures;

	private final ILexicon<MR>									lexicon;

	/**
	 * Listeners that follow the model updates. Listeners can be added or
	 * removed and they are not part of the serializable state of the model.
	 */
	private transient Set<IModelListener<MR>>					listeners			= new HashSet<IModelListener<MR>>();

	private final IHashVector									theta;

	protected Model(List<IParseFeatureSet<DI, MR>> featureSets,
			ILexicon<MR> lexicon, IHashVector theta) {
		this.featureSets = Collections.unmodifiableList(featureSets);
		final List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFS = new ArrayList<IIndependentLexicalFeatureSet<DI, MR>>();
		final Set<KeyArgs> invalidFeatureKeys = new HashSet<KeyArgs>();
		for (final IParseFeatureSet<DI, MR> fs : featureSets) {
			invalidFeatureKeys.addAll(fs.getDefaultFeatures());
			if (fs instanceof IIndependentLexicalFeatureSet) {
				lexicalFS.add((IIndependentLexicalFeatureSet<DI, MR>) fs);
			}
		}
		this.invalidFeatures = Collections.unmodifiableSet(invalidFeatureKeys);
		this.theta = theta;
		// Set the weights of the default features to 1.0. This will guarantee
		// that once their values are set properly by the respective feature
		// sets, various computations (such as multiplying theta by a feature
		// vector) will return the correct result.
		for (final KeyArgs key : invalidFeatures) {
			theta.set(key, 1.0);
		}
		this.independentLexicalFeatureSets = Collections
				.unmodifiableList(lexicalFS);
		this.lexicon = lexicon;
		LOG.info("Init %s :: independentLexicalFeatureSets=%s",
				Model.class.getSimpleName(), independentLexicalFeatureSets);
		LOG.info(".... %s :: featureSets=%s", Model.class.getSimpleName(),
				featureSets);
		LOG.info(".... %s :: lexiconClass=%s", Model.class.getSimpleName(),
				lexicon.getClass().getSimpleName());
		LOG.info(".... %s :: paramClass=%s", Model.class.getSimpleName(),
				theta.getClass().getSimpleName());
	}

	/**
	 * Read {@link Model} object from a file.
	 */
	public static <DI extends IDataItem<?>, MR> Model<DI, MR> readModel(
			File file) throws ClassNotFoundException, IOException {
		LOG.info("Reading model from file...");
		final long start = System.currentTimeMillis();
		try (final ObjectInput input = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(file)))) {
			@SuppressWarnings("unchecked")
			final Model<DI, MR> model = (Model<DI, MR>) input.readObject();
			LOG.info("Model loaded. Reading time: %.4f",
					(System.currentTimeMillis() - start) / 1000.0);
			return model;
		}
	}

	/**
	 * Store model object in a file.
	 *
	 * @throws IOException
	 */
	public static <DI extends IDataItem<?>, MR> void write(
			IModelImmutable<DI, MR> model, File file) throws IOException {
		try (final OutputStream os = new FileOutputStream(file);
				final OutputStream buffer = new BufferedOutputStream(os);
				final ObjectOutput output = new ObjectOutputStream(buffer)) {
			output.writeObject(model);
		}
	}

	/**
	 * Adds a batch of lexical items. The items are indifferent to one another
	 * when getting their initial scores.
	 *
	 * @param entries
	 * @return 'true' iff at least one new entry was introduced to the lexicon.
	 */
	public boolean addLexEntries(Collection<LexicalEntry<MR>> entries) {
		final Set<LexicalEntry<MR>> addedEntries = new HashSet<>();
		for (final LexicalEntry<MR> entry : entries) {
			if (entry.isDynamic()) {
				throw new IllegalStateException(
						"Trying to add a dynmic entry to the model: " + entry);
			}
			addedEntries.addAll(lexicon.add(entry));
		}
		for (final IParseFeatureSet<DI, MR> fs : featureSets) {
			if (fs instanceof ILexicalFeatureSet) {
				for (final LexicalEntry<MR> entry : addedEntries) {
					((ILexicalFeatureSet<DI, MR>) fs).addEntry(entry, theta);
				}
			}
		}
		if (!addedEntries.isEmpty()) {
			for (final IModelListener<MR> listener : listeners) {
				listener.lexicalEntriesAdded(addedEntries);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Add a lexical entry to the lexicon and update the feature vector. Will
	 * not add already existing ones.
	 *
	 * @param entry
	 * @return 'true' iff a new entry was introduced to the lexicon.
	 */
	public boolean addLexEntry(LexicalEntry<MR> entry) {
		if (entry.isDynamic()) {
			throw new IllegalStateException(
					"Trying to add a dynmic entry to the model: " + entry);
		}

		final Set<LexicalEntry<MR>> addedEntries = lexicon.add(entry);
		for (final IParseFeatureSet<DI, MR> fs : featureSets) {
			if (fs instanceof ILexicalFeatureSet) {
				for (final LexicalEntry<MR> addedEntry : addedEntries) {
					((ILexicalFeatureSet<DI, MR>) fs).addEntry(addedEntry,
							theta);
				}
			}
		}
		if (!addedEntries.isEmpty()) {
			for (final IModelListener<MR> listener : listeners) {
				listener.lexicalEntriesAdded(addedEntries);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep, DI dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IParseFeatureSetImmutable<DI, MR> featureSet : featureSets) {
			featureSet.setFeatures(parseStep, features, dataItem);
		}
		return features;
	}

	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry) {
		final IHashVector features = HashVectorFactory.create();
		for (final IIndependentLexicalFeatureSet<DI, MR> lfs : independentLexicalFeatureSets) {
			lfs.setFeatures(lexicalEntry, features);
		}
		return features;
	}

	@Override
	public IDataItemModel<MR> createDataItemModel(DI dataItem) {
		return new DataItemModel<DI, MR>(this, dataItem);
	}

	@Override
	public ILexicon<MR> getLexicon() {
		return lexicon;
	}

	public List<IParseFeatureSet<DI, MR>> getParseFeatures() {
		return featureSets;
	}

	@Override
	public IHashVector getTheta() {
		return theta;
	}

	@Override
	public boolean isValidWeightVector(IHashVectorImmutable vector) {
		for (final Pair<KeyArgs, Double> entry : vector) {
			if (invalidFeatures.contains(entry.first())) {
				return false;
			}
		}
		return true;
	}

	public void registerListener(IModelListener<MR> listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public double score(IHashVectorImmutable features) {
		return theta.dotProduct(features);
	}

	@Override
	public double score(LexicalEntry<MR> entry) {
		return score(computeFeatures(entry));
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("Feature sets:\n");
		for (final IParseFeatureSet<DI, MR> featureSet : featureSets) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append(
				"Independent Lexical feature sets (subset of all features):\n");
		for (final ILexicalFeatureSet<DI, MR> featureSet : independentLexicalFeatureSets) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Lexicon [size=").append(lexicon.size()).append("]\n");
		ret.append(lexiconToString(lexicon));
		ret.append("Feature vector\n").append(theta);

		return ret.toString();
	}

	public void unregisterListener(IModelListener<MR> listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private String lexiconToString(ILexicon<MR> lex) {
		final StringBuffer result = new StringBuffer();
		final Iterator<LexicalEntry<MR>> i = lex.toCollection().iterator();
		while (i.hasNext()) {
			final LexicalEntry<MR> entry = i.next();
			result.append(entry);
			result.append(" [").append(score(entry)).append("]");

			result.append("\n");
		}
		return result.toString();
	}

	/**
	 * @param ois
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		// Create an empty set for listeners to register.
		this.listeners = new HashSet<>();
	}

	public static class Builder<DI extends IDataItem<?>, MR> {
		private final List<IParseFeatureSet<DI, MR>>	featureSets	= new LinkedList<IParseFeatureSet<DI, MR>>();
		private ILexicon<MR>							lexicon		= new Lexicon<MR>();

		public Builder<DI, MR> addFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			featureSets.add(featureSet);
			return this;
		}

		public Model<DI, MR> build() {
			return new Model<DI, MR>(featureSets, lexicon,
					HashVectorFactory.create());
		}

		public Builder<DI, MR> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR>
			implements IResourceObjectCreator<Model<DI, MR>> {

		@SuppressWarnings("unchecked")
		@Override
		public Model<DI, MR> create(Parameters params,
				IResourceRepository repo) {

			// Case loading from file.
			if (params.contains("file")) {
				try {
					LOG.info("Loading model from: %s",
							params.getAsFile("file").getAbsolutePath());
					return Model.readModel(params.getAsFile("file"));
				} catch (final ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				// Case creating a new model.
				final Builder<DI, MR> builder = new Model.Builder<DI, MR>();

				// Lexicon.
				if (params.contains("lexicon")) {
					builder.setLexicon(
							(ILexicon<MR>) repo.get(params.get("lexicon")));
				}

				// Feature sets.
				for (final String setId : params.getSplit("features")) {
					builder.addFeatureSet(
							(IParseFeatureSet<DI, MR>) repo.get(setId));
				}

				final Model<DI, MR> model = builder.build();

				return model;
			}
		}

		@Override
		public String type() {
			return "model";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), Model.class)
					.setDescription(
							"Parsing model, including lexicon, features and a weight vector")
					.addParam("lexicon", "id", "Lexicon to use with this model")
					.addParam("lexicalFeatures", "[id]",
							"Lexical feature sets to use (e.g., 'lfs1,lfs2,lfs3')")
					.addParam("parseFeatures", "[id]",
							"Parse feature sets to use (e.g., 'pfs1,pfs2,pfs3')")
					.build();
		}

		protected ILexicon<MR> createLexicon(String lexiconType) {
			if ("conventional".equals(lexiconType)) {
				return new Lexicon<MR>();
			} else {
				throw new IllegalArgumentException(
						"Invalid lexicon type: " + lexiconType);
			}
		}

	}
}
