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
package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;

/**
 * AP => S\S
 * 
 * @author Yoav Artzi
 */
public class AdverbialTypeShifting extends AbstractUnaryRuleForThreading {
	private static final Syntax	S_BS_S_SYNTAX	= new ComplexSyntax(Syntax.S,
														Syntax.S,
														Slash.BACKWARD);
	
	public AdverbialTypeShifting() {
		this("shift_ap");
	}
	
	public AdverbialTypeShifting(String name) {
		super(name, Syntax.AP);
	}
	
	@Override
	protected Syntax getSourceSyntax() {
		return Syntax.AP;
	}
	
	@Override
	protected Syntax getTargetSyntax() {
		return S_BS_S_SYNTAX;
	}
	
	public static class Creator implements
			IResourceObjectCreator<AdverbialTypeShifting> {
		
		private final String	type;
		
		public Creator() {
			this("rule.shifting.ap");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public AdverbialTypeShifting create(Parameters params,
				IResourceRepository repo) {
			return new AdverbialTypeShifting();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, AdverbialTypeShifting.class)
					.build();
		}
		
	}
	
}
