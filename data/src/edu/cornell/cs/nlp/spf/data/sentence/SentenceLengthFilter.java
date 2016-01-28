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
package edu.cornell.cs.nlp.spf.data.sentence;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.filter.IFilter;

/**
 * Filter sentences based on the number of tokens, given a maximum length.
 *
 * @author Yoav Artzi
 */
public class SentenceLengthFilter<DI extends IDataItem<Sentence>> implements
		IFilter<DI> {

	private final int	lengthThreshold;

	public SentenceLengthFilter(int lengthThreshold) {
		this.lengthThreshold = lengthThreshold;
	}

	@Override
	public boolean test(DI e) {
		return e.getSample().getTokens().size() <= lengthThreshold;
	}

	public static class Creator<DI extends IDataItem<Sentence>> implements
			IResourceObjectCreator<SentenceLengthFilter<DI>> {

		private final String	type;

		public Creator() {
			this("filter.sentence.length");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public SentenceLengthFilter<DI> create(Parameters params,
				IResourceRepository repo) {
			return new SentenceLengthFilter<DI>(Integer.valueOf(params
					.get("length")));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, SentenceLengthFilter.class)
					.addParam("length", "int",
							"Max number of tokens for valid sentences").build();
		}

	}

}
