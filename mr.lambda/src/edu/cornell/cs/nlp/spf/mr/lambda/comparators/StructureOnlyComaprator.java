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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetStructure;

/**
 * Compare the structure of the logical form. Ignores constant names (but not
 * types).
 * 
 * @author Yoav Artzi
 */
public class StructureOnlyComaprator implements ILogicalExpressionComparator {
	
	private final ILogicalExpressionComparator	baseComparator;
	
	public StructureOnlyComaprator(ILogicalExpressionComparator baseComparator) {
		this.baseComparator = baseComparator;
	}
	
	@Override
	public boolean compare(LogicalExpression o1, LogicalExpression o2) {
		final LogicalExpression anonO1 = GetStructure.of(o1);
		final LogicalExpression anonO2 = GetStructure.of(o2);
		return baseComparator.compare(anonO1, anonO2);
	}
	
	public static class Creator implements
			IResourceObjectCreator<StructureOnlyComaprator> {
		
		private final String	type;
		
		public Creator() {
			this("comparator.structonly");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public StructureOnlyComaprator create(Parameters params,
				IResourceRepository repo) {
			return new StructureOnlyComaprator(
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
					StructureOnlyComaprator.class).addParam("comparator",
					ILogicalExpressionComparator.class,
					"Base comparator to use (default is taken from LLS)")
					.build();
		}
		
	}
	
}
