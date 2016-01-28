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

import java.io.File;
import java.io.IOException;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Initialize the weights of arbitrary features a file of feature names and
 * values. Each line in the file is of the form <FEATURE_NAME>=<VALUE>. Lines
 * can be commented with '//'.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item.
 * @param <MR>
 *            Meaning representation.
 */
public class WeightInit<DI extends IDataItem<?>, MR>
		implements IModelInit<DI, MR> {

	public static final ILogger LOG = LoggerFactory.create(WeightInit.class);

	private final IHashVector initWeight;

	public WeightInit(File file) throws IOException {
		this.initWeight = HashVectorFactory.read(file);
	}

	@Override
	public void init(Model<DI, MR> model) {
		for (final Pair<KeyArgs, Double> weightEntry : initWeight) {
			LOG.info("Set: %s -> %s", weightEntry.first(),
					weightEntry.second());
			model.getTheta().set(weightEntry.first(), weightEntry.second());
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR>
			implements IResourceObjectCreator<WeightInit<DI, MR>> {

		private String type;

		public Creator() {
			this("init.weights");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public WeightInit<DI, MR> create(Parameters params,
				IResourceRepository repo) {
			try {
				return new WeightInit<DI, MR>(params.getAsFile("file"));
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, WeightInit.class)
					.addParam("file", File.class,
							"Tab separated key-value file, each lines contains a single pair.")
					.setDescription("Model init to set weights given a file.")
					.build();
		}

	}

}
