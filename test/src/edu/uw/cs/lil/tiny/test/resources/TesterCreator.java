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
package edu.uw.cs.lil.tiny.test.resources;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.test.Tester;
import edu.uw.cs.utils.filter.IFilter;

public class TesterCreator<SAMPLE, MR> implements
		IResourceObjectCreator<Tester<SAMPLE, MR>> {
	
	@SuppressWarnings("unchecked")
	@Override
	public Tester<SAMPLE, MR> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		
		// Get the testing set
		final IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>> testSet;
		{
			// [yoav] [17/10/2011] Store in Object to javac known bug
			final Object dataCollection = resourceRepo.getResource(parameters
					.get("data"));
			if (dataCollection == null
					|| !(dataCollection instanceof IDataCollection<?>)) {
				throw new RuntimeException("Unknown or non labeled dataset: "
						+ parameters.get("data"));
			} else {
				testSet = (IDataCollection<? extends ILabeledDataItem<SAMPLE, MR>>) dataCollection;
			}
		}
		
		if (!parameters.contains("parser")) {
			throw new IllegalStateException(
					"tester now requires you to provide a parser");
		}
		
		final Tester.Builder<SAMPLE, MR> builder = new Tester.Builder<SAMPLE, MR>(
				testSet,
				(IParser<SAMPLE, MR>) resourceRepo.getResource(parameters
						.get("parser")));
		
		if (parameters.get("skippingFilter") != null) {
			builder.setSkipParsingFilter((IFilter<SAMPLE>) resourceRepo
					.getResource(parameters.get("skippingFilter")));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return "tester";
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), Tester.class)
				.setDescription(
						"Model tester. Tests inference using the model on some testing data")
				.addParam("data", "id",
						"IDataCollection that holds ILabaledDataItem entries")
				.addParam("parser", "id", "Parser object")
				.addParam("skippingFilter", "id",
						"IFilter used to decide which data items to skip")
				.build();
	}
	
}
