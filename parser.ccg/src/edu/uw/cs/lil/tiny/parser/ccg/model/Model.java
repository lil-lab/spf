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
package edu.uw.cs.lil.tiny.parser.ccg.model;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.IParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.IIndependentLexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.ILexicalFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSet;
import edu.uw.cs.lil.tiny.parser.ccg.model.parse.IParseFeatureSetImmutable;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * A complete parsing model, including features, parameters and a lexicon.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Data item used for inference.
 * @param <MR>
 *            Type of semantics (output).
 */
public class Model<DI extends IDataItem<?>, MR> implements
		IModelImmutable<DI, MR> {
	public static final ILogger									LOG					= LoggerFactory
																							.create(Model.class);
	
	private static final long									serialVersionUID	= -2202596624826388636L;
	
	private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures;
	
	private final ILexicon<MR>									lexicon;
	
	private final List<IParseFeatureSet<DI, MR>>				parseFeatures;
	
	private final IHashVector									theta;
	
	protected Model(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures, ILexicon<MR> lexicon) {
		this(lexicalFeatures, parseFeatures, lexicon, HashVectorFactory
				.create());
	}
	
	protected Model(
			List<IIndependentLexicalFeatureSet<DI, MR>> lexicalFeatures,
			List<IParseFeatureSet<DI, MR>> parseFeatures, ILexicon<MR> lexicon,
			IHashVector theta) {
		this.lexicalFeatures = Collections.unmodifiableList(lexicalFeatures);
		this.parseFeatures = Collections.unmodifiableList(parseFeatures);
		this.lexicon = lexicon;
		this.theta = theta;
		LOG.info("Init %s :: lexicalFeatures=%s", Model.class.getSimpleName(),
				lexicalFeatures);
		LOG.info(".... %s :: parseFeatures=%s", Model.class.getSimpleName(),
				parseFeatures);
		LOG.info(".... %s :: lexiconClass=%s", Model.class.getSimpleName(),
				lexicon.getClass().getSimpleName());
	}
	
	/**
	 * Read model object from a file.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static <DI extends IDataItem<?>, MR> Model<DI, MR> read(File file)
			throws ClassNotFoundException, IOException {
		final ObjectInput input = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(file)));
		@SuppressWarnings("unchecked")
		final Model<DI, MR> model = (Model<DI, MR>) input.readObject();
		input.close();
		return model;
	}
	
	/**
	 * Store model object in a file.
	 * 
	 * @throws IOException
	 */
	public static <DI extends IDataItem<?>, MR> void write(Model<DI, MR> model,
			File file) throws IOException {
		final OutputStream os = new FileOutputStream(file);
		final OutputStream buffer = new BufferedOutputStream(os);
		final ObjectOutput output = new ObjectOutputStream(buffer);
		output.writeObject(model);
		output.close();
		buffer.close();
		os.close();
	}
	
	/**
	 * Adds a batch of lexical items. The items are indifferent to one another
	 * when getting their initial scores.
	 * 
	 * @param entries
	 * @return 'true' iff at least one new entry was introduced to the lexicon.
	 */
	public boolean addLexEntries(Collection<LexicalEntry<MR>> entries) {
		final Set<LexicalEntry<MR>> addedEntries = lexicon.addAll(entries);
		for (final LexicalEntry<MR> entry : addedEntries) {
			for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
				lfs.addEntry(entry, theta);
			}
		}
		return !addedEntries.isEmpty();
	}
	
	/**
	 * Add a lexical entry to the lexicon and update the feature vector. Will
	 * not add already existing ones.
	 * 
	 * @param entry
	 * @return 'true' iff a new entry was introduced to the lexicon.
	 */
	public boolean addLexEntry(LexicalEntry<MR> entry) {
		final Set<LexicalEntry<MR>> addedEntries = lexicon.add(entry);
		for (final LexicalEntry<MR> addedEntry : addedEntries) {
			for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
				lfs.addEntry(addedEntry, theta);
			}
		}
		return !addedEntries.isEmpty();
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep, DI dataItem) {
		return computeFeatures(parseStep, HashVectorFactory.create(), dataItem);
	}
	
	@Override
	public IHashVector computeFeatures(IParseStep<MR> parseStep,
			IHashVector features, DI dataItem) {
		for (final IParseFeatureSetImmutable<DI, MR> featureSet : parseFeatures) {
			featureSet.setFeats(parseStep, features, dataItem);
		}
		for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
			lfs.setFeats(parseStep, features, dataItem);
		}
		return features;
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry) {
		return computeFeatures(lexicalEntry, HashVectorFactory.create());
	}
	
	@Override
	public IHashVector computeFeatures(LexicalEntry<MR> lexicalEntry,
			IHashVector features) {
		for (final IIndependentLexicalFeatureSet<DI, MR> lfs : lexicalFeatures) {
			lfs.setFeats(lexicalEntry, features);
		}
		return features;
	}
	
	@Override
	public IDataItemModel<MR> createDataItemModel(DI dataItem) {
		return new DataItemModel<DI, MR>(this, dataItem);
	}
	
	public List<IIndependentLexicalFeatureSet<DI, MR>> getLexicalFeatures() {
		return lexicalFeatures;
	}
	
	@Override
	public ILexicon<MR> getLexicon() {
		return lexicon;
	}
	
	public List<IParseFeatureSet<DI, MR>> getParseFeatures() {
		return parseFeatures;
	}
	
	@Override
	public IHashVector getTheta() {
		return theta;
	}
	
	public boolean isValidWeightVector(IHashVectorImmutable vector) {
		for (final IIndependentLexicalFeatureSet<DI, MR> set : lexicalFeatures) {
			if (!set.isValidWeightVector(vector)) {
				return false;
			}
		}
		for (final IParseFeatureSet<DI, MR> set : parseFeatures) {
			if (!set.isValidWeightVector(vector)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public double score(IParseStep<MR> parseStep, DI dataItem) {
		double score = 0.0;
		// Parse features.
		for (final IParseFeatureSet<DI, MR> featureSet : parseFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		// Lexical features.
		for (final IIndependentLexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			score += featureSet.score(parseStep, theta, dataItem);
		}
		return score;
	}
	
	@Override
	public double score(LexicalEntry<MR> entry) {
		double score = 0.0;
		for (final IIndependentLexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			score += featureSet.score(entry, theta);
		}
		return score;
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("Lexical feature sets:\n");
		for (final ILexicalFeatureSet<DI, MR> featureSet : lexicalFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Parse feature sets:\n");
		for (final IParseFeatureSet<DI, MR> featureSet : parseFeatures) {
			ret.append("\t").append(featureSet).append("\n");
		}
		ret.append("Lexicon [size=").append(lexicon.size()).append("]\n");
		ret.append(lexiconToString(lexicon));
		ret.append("Feature vector\n").append(theta);
		
		return ret.toString();
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
	
	public static class Builder<DI extends IDataItem<?>, MR> {
		private final List<IIndependentLexicalFeatureSet<DI, MR>>	lexicalFeatures	= new LinkedList<IIndependentLexicalFeatureSet<DI, MR>>();
		private ILexicon<MR>										lexicon			= new Lexicon<MR>();
		private final List<IParseFeatureSet<DI, MR>>				parseFeatures	= new LinkedList<IParseFeatureSet<DI, MR>>();
		
		public Builder<DI, MR> addLexicalFeatureSet(
				IIndependentLexicalFeatureSet<DI, MR> featureSet) {
			lexicalFeatures.add(featureSet);
			return this;
		}
		
		public Builder<DI, MR> addParseFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			parseFeatures.add(featureSet);
			return this;
		}
		
		public Model<DI, MR> build() {
			return new Model<DI, MR>(
					Collections.unmodifiableList(lexicalFeatures),
					Collections.unmodifiableList(parseFeatures), lexicon);
		}
		
		public Builder<DI, MR> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}
	}
	
	public static class Creator<DI extends IDataItem<?>, MR> implements
			IResourceObjectCreator<Model<DI, MR>> {
		
		@SuppressWarnings("unchecked")
		@Override
		public Model<DI, MR> create(Parameters params, IResourceRepository repo) {
			
			// Case loading from file.
			if (params.contains("file")) {
				try {
					LOG.info("Loading model from: %s", params.getAsFile("file")
							.getAbsolutePath());
					return Model.read(params.getAsFile("file"));
				} catch (final ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				// Case creating a new model.
				
				final Builder<DI, MR> builder = new Model.Builder<DI, MR>();
				
				// Lexicon
				builder.setLexicon((ILexicon<MR>) repo.getResource(params
						.get("lexicon")));
				
				// Lexical feature sets
				for (final String setId : params.getSplit("lexicalFeatures")) {
					builder.addLexicalFeatureSet((IIndependentLexicalFeatureSet<DI, MR>) repo
							.getResource(setId));
				}
				
				// Parse feature sets
				for (final String setId : params.getSplit("parseFeatures")) {
					builder.addParseFeatureSet((IParseFeatureSet<DI, MR>) repo
							.getResource(setId));
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
				throw new IllegalArgumentException("Invalid lexicon type: "
						+ lexiconType);
			}
		}
		
	}
}
