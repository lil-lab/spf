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
package edu.cornell.cs.nlp.spf.mr.lambda;

import jregex.Pattern;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.comparators.SkolemIdInstanceWrapper;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;

/**
 * ID to uniquely identify an entity.
 *
 * @author Yoav Artzi
 */
public class SkolemId extends Variable {

	private static final char		MARKER				= '!';
	private static final Pattern	REGEXP_PATTERN		= new Pattern(
																String.valueOf(MARKER)
																		+ "\\d+");
	private static final long		serialVersionUID	= -1844431624191684757L;

	public SkolemId() {
		super(SkolemServices.getIDType());
	}

	@Override
	public boolean equals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (!(exp instanceof SkolemId)) {
			return false;
		}

		final Variable mappedValue = mapping.peek(this);
		if (mappedValue == exp && mapping.peekValue(mappedValue) == this) {
			return true;
		} else if (exp instanceof SkolemIdInstanceWrapper) {
			return exp.equals(this, mapping);
		} else if (!mapping.containsValue((SkolemId) exp)) {
			mapping.push(this, (SkolemId) exp);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean equals(Object obj) {
		// Similarly to variable, without any mapping, IDs are only equal when
		// they are identical.
		return obj == this;
	}

	public String getName(int assignedNumber) {
		return String.valueOf(MARKER) + String.valueOf(assignedNumber);
	}

	@Override
	protected boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		return equals(exp, mapping);
	}

	public static class Reader implements IReader<SkolemId> {

		@Override
		public SkolemId read(String string,
				ScopeMapping<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader) {
			if (!mapping.containsKey(string)) {
				mapping.push(string, new SkolemId());
			}
			return (SkolemId) mapping.peek(string);
		}

		@Override
		public boolean test(String string) {
			return REGEXP_PATTERN.matches(string);
		}

	}

}
