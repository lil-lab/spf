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

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;

/**
 * N : f:<e,t> => NP (a:<<e,t>,e> f)
 * 
 * @author Luke Zettlemoyer
 */
public class PluralTypeShifting extends AbstractUnaryRuleForThreading {
	
	private final LogicalConstant	aPred;
	private final Type				eToT;
	
	public PluralTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices) {
		this(categoryServices, "shift_plural");
	}
	
	public PluralTypeShifting(
			ICategoryServices<LogicalExpression> categoryServices, String name) {
		super(name, Syntax.N);
		this.eToT = LogicLanguageServices.getTypeRepository()
				.getTypeCreateIfNeeded("<e,t>");
		this.aPred = (LogicalConstant) categoryServices
				.parseSemantics("a:<<e,t>,e>");
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PluralTypeShifting other = (PluralTypeShifting) obj;
		if (aPred == null) {
			if (other.aPred != null) {
				return false;
			}
		} else if (!aPred.equals(other.aPred)) {
			return false;
		}
		if (eToT == null) {
			if (other.eToT != null) {
				return false;
			}
		} else if (!eToT.equals(other.eToT)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((aPred == null) ? 0 : aPred.hashCode());
		result = prime * result + ((eToT == null) ? 0 : eToT.hashCode());
		return result;
	}
	
	@Override
	protected Syntax getSourceSyntax() {
		return Syntax.N;
	}
	
	@Override
	protected Syntax getTargetSyntax() {
		return Syntax.NP;
	}
	
	@Override
	protected LogicalExpression typeShiftSemantics(LogicalExpression sem) {
		if (sem.getType().isExtendingOrExtendedBy(eToT)) {
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					1);
			args.add(sem);
			return new Literal(aPred, args);
		}
		return null;
	}
}
