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
package edu.uw.cs.lil.tiny.parser.resources;

import java.io.IOException;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.storage.DecoderHelper;
import edu.uw.cs.lil.tiny.storage.DecoderServices;

public class SavedModelCreator<DI extends IDataItem<?>, MR> implements
		IResourceObjectCreator<Model<DI, MR>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public Model<DI, MR> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		try {
			return DecoderServices
					.decode(parameters.getAsFile("dir"),
							(DecoderHelper<MR>) resourceRepo
									.getResource(ParameterizedExperiment.DECODER_HELPER_RESOURCE));
		} catch (final IOException e) {
			throw new IllegalStateException("failed to load model from: "
					+ parameters.getAsFile("dir"));
		}
	}
	
	@Override
	public String type() {
		return "model.saved";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), Model.class)
				.setDescription("Loads a saved model.")
				.addParam("dir", "directory",
						"Directory containing a saved model").build();
	}
	
}
