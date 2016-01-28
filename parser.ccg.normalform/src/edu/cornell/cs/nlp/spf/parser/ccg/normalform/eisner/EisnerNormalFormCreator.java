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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform.eisner;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.INormalFormConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator.Builder;

public class EisnerNormalFormCreator implements
		IResourceObjectCreator<NormalFormValidator> {
	
	private final String	type;
	
	public EisnerNormalFormCreator() {
		this("nf.eisner");
	}
	
	public EisnerNormalFormCreator(String type) {
		this.type = type;
	}
	
	@Override
	public NormalFormValidator create(Parameters params,
			IResourceRepository repo) {
		final Builder builder = new NormalFormValidator.Builder()
				.addConstraint(new EisnerConstraint());
		
		for (final String id : params.getSplit("extraConstraints")) {
			builder.addConstraint((INormalFormConstraint) repo.get(id));
		}
		
		return builder.build();
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type, NormalFormValidator.class)
				.setDescription(
						"Normal form parsing constraints following Eisner 1995.")
				.addParam("extraConstraints", INormalFormConstraint.class,
						"List of extra constraints to include.").build();
	}
	
}
