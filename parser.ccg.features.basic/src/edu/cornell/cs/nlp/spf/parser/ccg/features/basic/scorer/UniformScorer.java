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
package edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;

/**
 * Scores an object using a pre-defined constant value.
 * 
 * @author Luke Zettlemoyer
 * @param <T>
 *            Type of object to be scored.
 */
public class UniformScorer<T> implements ISerializableScorer<T> {
	
	private static final long	serialVersionUID	= 5896129775849488211L;
	
	private final double		score;
	
	public UniformScorer(double value) {
		score = value;
	}
	
	@Override
	public double score(T lex) {
		return score;
	}
	
	public static class Creator<T> implements
			IResourceObjectCreator<UniformScorer<T>> {
		
		private String	type;
		
		public Creator() {
			this("scorer.uniform");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public UniformScorer<T> create(Parameters parameters,
				IResourceRepository resourceRepo) {
			return new UniformScorer<T>(parameters.getAsDouble("weight"));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), UniformScorer.class)
					.setDescription("Uniform scoring function")
					.addParam("weight", "double",
							"Weight value. This weight will be given to any object the scorer recieves.")
					.build();
		}
		
	}
	
}
