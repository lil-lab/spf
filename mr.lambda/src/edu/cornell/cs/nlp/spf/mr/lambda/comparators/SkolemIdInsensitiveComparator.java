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
package edu.cornell.cs.nlp.spf.mr.lambda.comparators;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.ILogicalExpressionComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.StripSkolemIds;

/**
 * Compare logical expression, but ignore any difference in skolem IDs.
 * 
 * @author Yoav Artzi
 */
public class SkolemIdInsensitiveComparator implements
		ILogicalExpressionComparator {
	
	private final ILogicalExpressionComparator	comaprator;
	
	public SkolemIdInsensitiveComparator(ILogicalExpressionComparator comaprator) {
		this.comaprator = comaprator;
	}
	
	@Override
	public boolean compare(LogicalExpression o1, LogicalExpression o2) {
		return comaprator.compare(
				StripSkolemIds.of(o1, SkolemServices.getIdPlaceholder()),
				StripSkolemIds.of(o2, SkolemServices.getIdPlaceholder()));
	}
	
	public static class Creator implements
			IResourceObjectCreator<SkolemIdInsensitiveComparator> {
		
		private final String	type;
		
		public Creator() {
			this("comparator.skolem.insensitive");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public SkolemIdInsensitiveComparator create(Parameters params,
				IResourceRepository repo) {
			return new SkolemIdInsensitiveComparator(
					params.contains("comparator") ? (ILogicalExpressionComparator) repo
							.get("comparator") : LogicLanguageServices
							.getComparator());
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					StructureOnlyComaprator.class)
					.addParam("comparator", ILogicalExpressionComparator.class,
							"Base comparator to use (default is taken from LLS)")
					.addParam("placeholder", LogicalConstant.class,
							"Placeholder constant to use instead of skolem IDs.")
					.build();
		}
		
	}
}
