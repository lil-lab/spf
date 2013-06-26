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
package edu.uw.cs.lil.tiny.test.exec.resources;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.test.exec.ExecTester;
import edu.uw.cs.lil.tiny.test.exec.ExecTester.Builder;
import edu.uw.cs.utils.filter.IFilter;

public class ExecTesterCreator<X, Z> implements
		IResourceObjectCreator<ExecTester<X, Z>> {
	private static final String	DEFAULT_NAME	= "tester.exec";
	private final String		resourceName;
	
	public ExecTesterCreator() {
		this(DEFAULT_NAME);
	}
	
	public ExecTesterCreator(String resourceName) {
		this.resourceName = resourceName;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ExecTester<X, Z> create(Parameters params, IResourceRepository repo) {
		final Builder<X, Z> builder = new ExecTester.Builder<X, Z>();
		
		if (params.contains("sloppyFilter")) {
			builder.setSkipParsingFilter((IFilter<ILabeledDataItem<X, Z>>) repo
					.getResource(params.get("sloppyFilter")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return resourceName;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), ExecTester.class)
				.addParam(
						"sloppyFilter",
						"id",
						"IFilter used to decide what data items to skip when doing sloppy inference (e.g., skipping words)")
				.build();
	}
	
}
