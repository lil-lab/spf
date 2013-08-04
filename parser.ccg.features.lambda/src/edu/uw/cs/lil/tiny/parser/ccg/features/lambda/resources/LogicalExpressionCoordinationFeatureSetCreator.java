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
package edu.uw.cs.lil.tiny.parser.ccg.features.lambda.resources;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.features.lambda.LogicalExpressionCoordinationFeatureSet;

/**
 * Creator for {@link LogicalExpressionCoordinationFeatureSet}.
 * 
 * @author Yoav Artzi
 */
public class LogicalExpressionCoordinationFeatureSetCreator<DI extends IDataItem<?>>
		implements
		IResourceObjectCreator<LogicalExpressionCoordinationFeatureSet<DI>> {
	
	@Override
	public LogicalExpressionCoordinationFeatureSet<DI> create(
			Parameters params, IResourceRepository repo) {
		return new LogicalExpressionCoordinationFeatureSet<DI>(
				"true".equals(params.get("cpp1")), "true".equals(params
						.get("rept")), "true".equals(params.get("cpap")));
	}
	
	@Override
	public String type() {
		return "feat.logexp.coordination";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(),
				LogicalExpressionCoordinationFeatureSet.class)
				.setDescription(
						"Complete logical expression coordination features")
				.addParam("cpp1", "boolean",
						"Activate CPP1 features (options: true, false) (default: false)")
				.addParam("rept", "boolean",
						"Activate REPT features (options: true, false) (default: false)")
				.addParam("cpap", "boolean",
						"Activate CPAP features (options: true, false) (default: false)")
				.build();
	}
	
}
