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
package edu.cornell.cs.nlp.spf.parser.joint.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.situated.ISituatedDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.spf.parser.ccg.model.parse.IParseFeatureSet;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Model for joint inference of parsing and logical form evaluation/execution.
 * Extends Model in a way that all feature sets that are inside the parser see
 * the complete data item, including the initial world state. This allows
 * parsing features to use the state of the world, allowing for situated
 * inference.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item
 * @param <STATE>
 *            World state
 * @param <MR>
 *            Semantics
 * @param <ESTEP>
 *            Execution step
 */
public class JointModel<DI extends ISituatedDataItem<?, ?>, MR, ESTEP> extends
		Model<DI, MR> implements IJointModelImmutable<DI, MR, ESTEP> {
	public static ILogger							LOG					= LoggerFactory
																				.create(JointModel.class);

	private static final long						serialVersionUID	= 5991114566047733132L;

	private final List<IJointFeatureSet<DI, ESTEP>>	jointFeatures;

	protected JointModel(List<IParseFeatureSet<DI, MR>> featureSets,
			List<IJointFeatureSet<DI, ESTEP>> jointFeatures,
			ILexicon<MR> lexicon, IHashVector theta) {
		super(featureSets, lexicon, theta);
		this.jointFeatures = Collections.unmodifiableList(jointFeatures);
	}

	/**
	 * Read {@link JointModel} object from a file.
	 */
	public static <DI extends ISituatedDataItem<?, ?>, MR, ESTEP> JointModel<DI, MR, ESTEP> readJointModel(
			File file) throws ClassNotFoundException, IOException {
		LOG.info("Reading joint model from file...");
		final long start = System.currentTimeMillis();
		try (final ObjectInput input = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(file)))) {
			@SuppressWarnings("unchecked")
			final JointModel<DI, MR, ESTEP> model = (JointModel<DI, MR, ESTEP>) input
					.readObject();
			LOG.info("Model loaded. Reading time: %.4f",
					(System.currentTimeMillis() - start) / 1000.0);
			return model;
		}
	}

	@Override
	public IHashVector computeFeatures(ESTEP executionStep, DI dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IJointFeatureSet<DI, ESTEP> featureSet : jointFeatures) {
			featureSet.setFeats(executionStep, features, dataItem);
		}
		return features;
	}

	@Override
	public IJointDataItemModel<MR, ESTEP> createJointDataItemModel(DI dataItem) {
		return new JointDataItemModel<DI, MR, ESTEP>(this, dataItem);
	}

	public List<IJointFeatureSet<DI, ESTEP>> getJointFeatures() {
		return jointFeatures;
	}

	@Override
	public double score(ESTEP executionStep, DI dataItem) {
		return score(computeFeatures(executionStep, dataItem));
	}

	public static class Builder<DI extends ISituatedDataItem<?, ?>, MR, ESTEP> {

		private final List<IParseFeatureSet<DI, MR>>	featureSets		= new LinkedList<IParseFeatureSet<DI, MR>>();
		private final List<IJointFeatureSet<DI, ESTEP>>	jointFeatures	= new LinkedList<IJointFeatureSet<DI, ESTEP>>();
		private ILexicon<MR>							lexicon			= new Lexicon<MR>();

		public Builder<DI, MR, ESTEP> addFeatureSet(
				IParseFeatureSet<DI, MR> featureSet) {
			featureSets.add(featureSet);
			return this;
		}

		public Builder<DI, MR, ESTEP> addJointFeatureSet(
				IJointFeatureSet<DI, ESTEP> featureSet) {
			jointFeatures.add(featureSet);
			return this;
		}

		public JointModel<DI, MR, ESTEP> build() {
			return new JointModel<DI, MR, ESTEP>(featureSets, jointFeatures,
					lexicon, HashVectorFactory.create());
		}

		public Builder<DI, MR, ESTEP> setLexicon(ILexicon<MR> lexicon) {
			this.lexicon = lexicon;
			return this;
		}

	}

	public static class Creator<DI extends ISituatedDataItem<?, ?>, MR, ESTEP>
			implements IResourceObjectCreator<JointModel<DI, MR, ESTEP>> {

		@SuppressWarnings("unchecked")
		@Override
		public JointModel<DI, MR, ESTEP> create(Parameters params,
				IResourceRepository repo) {
			final JointModel.Builder<DI, MR, ESTEP> builder = new JointModel.Builder<DI, MR, ESTEP>();

			// Case loading from file.
			if (params.contains("file")) {
				try {
					LOG.info("Loading model from: %s", params.getAsFile("file")
							.getAbsolutePath());
					return JointModel.readJointModel(params.getAsFile("file"));
				} catch (final ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				// Lexicon.
				if (params.contains("lexicon")) {
					builder.setLexicon((ILexicon<MR>) repo.get(params
							.get("lexicon")));
				}

				// Parse feature sets.
				for (final String setId : params.getSplit("features")) {
					builder.addFeatureSet((IParseFeatureSet<DI, MR>) repo
							.get(setId));
				}

				// Joint feature sets.
				for (final String setId : params.getSplit("jointFeatures")) {
					builder.addJointFeatureSet((IJointFeatureSet<DI, ESTEP>) repo
							.get(setId));
				}

				final JointModel<DI, MR, ESTEP> model = builder.build();

				return model;
			}
		}

		@Override
		public String type() {
			return "model.joint";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), JointModel.class)
					.setDescription(
							"Model for joint inference of parsing and 'execution'")
					.addParam("lexicon", "id", "The model's lexicon (ILexicon)")
					.addParam(
							"lexicalFeatures",
							"ids",
							"Lexical features sets (IIndependentLexicalFeatureSet) (e.g., 'lfs1,lfs2,lfs3')")
					.addParam("parseFeatures", "ids",
							"Parsing feature sets (IParseFeatureSet) (e.g., 'pfs1,pfs2,pfs3')")
					.addParam(
							"jointFeatures",
							"ids",
							"Joint feature sets to be used in execution (IJointFeatureSet) (e.g., 'jfs1,jfs2,jfs3')")
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
