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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.basic;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * Type shifting: S -> S/AP
 * 
 * @author Yoav Artzi
 */
public class SententialAdverbialTypeShifting extends
		AbstractTypeShiftingFunctionForThreading {
	private static final Syntax	S_FS_AP_SYNTAX	= new ComplexSyntax(Syntax.S,
														Syntax.AP,
														Slash.FORWARD);
	private final String		name;
	
	public SententialAdverbialTypeShifting() {
		this("shift_s_ap");
	}
	
	public SententialAdverbialTypeShifting(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SententialAdverbialTypeShifting other = (SententialAdverbialTypeShifting) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public Category<LogicalExpression> typeShift(
			Category<LogicalExpression> category) {
		if (category.getSyntax().equals(Syntax.S)) {
			final LogicalExpression raisedSemantics = typeRaiseSemantics(category
					.getSem());
			if (raisedSemantics != null) {
				return Category.create(S_FS_AP_SYNTAX, raisedSemantics);
			}
		}
		return null;
	}
	
	public static class Creator implements
			IResourceObjectCreator<SententialAdverbialTypeShifting> {
		
		private final String	type;
		
		public Creator() {
			this("rule.shifting.sentence.ap");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public SententialAdverbialTypeShifting create(Parameters params,
				IResourceRepository repo) {
			return new SententialAdverbialTypeShifting();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					SententialAdverbialTypeShifting.class).build();
		}
		
	}
}
