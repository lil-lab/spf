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

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Wrap a {@link SkolemId} so {@link #equals(Object)} will do instance
 * comparison rather than use (or update) the existing mapping.
 *
 * @author Yoav Artzi
 */
public class SkolemIdInstanceWrapper extends SkolemId {

	private static final long	serialVersionUID	= 8287647752712804966L;
	private final SkolemId		base;

	private SkolemIdInstanceWrapper(SkolemId base) {
		this.base = base;
	}

	public static SkolemIdInstanceWrapper of(SkolemId base) {
		return new SkolemIdInstanceWrapper(base);
	}

	public static LogicalExpression unwrap(LogicalExpression exp) {
		final UnwrapIds visitor = new UnwrapIds();
		visitor.visit(exp);
		return visitor.result;
	}

	public static LogicalExpression wrapIds(LogicalExpression exp) {
		final WrapIds visitor = new WrapIds();
		visitor.visit(exp);
		return visitor.result;
	}

	@Override
	public int calcHashCode() {
		return base.calcHashCode();
	}

	@Override
	public boolean equals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (exp instanceof SkolemIdInstanceWrapper) {
			return base == ((SkolemIdInstanceWrapper) exp).base;
		} else {
			return base == exp;
		}
	}

	@Override
	public String getName(int assignedNumber) {
		return base.getName(assignedNumber);
	}

	@Override
	public Type getType() {
		return base.getType();
	}

	private static class UnwrapIds implements ILogicalExpressionVisitor {

		private LogicalExpression	result	= null;

		@Override
		public void visit(Lambda lambda) {
			lambda.getArgument().accept(this);
			final Variable argument = (Variable) result;
			lambda.getBody().accept(this);
			if (result != lambda.getBody() || argument != lambda.getArgument()) {
				result = new Lambda(argument, result);
			} else {
				result = lambda;
			}
		}

		@Override
		public void visit(Literal literal) {
			literal.getPredicate().accept(this);
			final LogicalExpression predicate = result;
			final int len = literal.numArgs();
			final LogicalExpression[] newArgs = new LogicalExpression[len];
			boolean argChanged = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				arg.accept(this);
				newArgs[i] = result;
				if (result != arg) {
					argChanged = true;
				}
			}

			if (!argChanged && literal.getPredicate() == predicate) {
				result = literal;
			} else if (argChanged) {
				result = new Literal(predicate, newArgs);
			} else {
				result = new Literal(predicate, literal);
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			result = logicalConstant;
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			if (variable instanceof SkolemIdInstanceWrapper) {
				result = ((SkolemIdInstanceWrapper) variable).base;
			} else {
				result = variable;
			}
		}

	}

	private static class WrapIds implements ILogicalExpressionVisitor {

		private LogicalExpression	result	= null;

		@Override
		public void visit(Lambda lambda) {
			lambda.getArgument().accept(this);
			final Variable argument = (Variable) result;
			lambda.getBody().accept(this);
			if (result != lambda.getBody() || argument != lambda.getArgument()) {
				result = new Lambda(argument, result);
			} else {
				result = lambda;
			}
		}

		@Override
		public void visit(Literal literal) {
			literal.getPredicate().accept(this);
			final LogicalExpression predicate = result;
			final int len = literal.numArgs();
			final LogicalExpression[] newArgs = new LogicalExpression[len];
			boolean argChanged = false;
			for (int i = 0; i < len; ++i) {
				final LogicalExpression arg = literal.getArg(i);
				arg.accept(this);
				newArgs[i] = result;
				if (result != arg) {
					argChanged = true;
				}
			}

			if (!argChanged && literal.getPredicate() == predicate) {
				result = literal;
			} else if (argChanged) {
				result = new Literal(predicate, newArgs);
			} else {
				result = new Literal(predicate, literal);
			}
		}

		@Override
		public void visit(LogicalConstant logicalConstant) {
			result = logicalConstant;
		}

		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}

		@Override
		public void visit(Variable variable) {
			if (variable instanceof SkolemId
					&& !(variable instanceof SkolemIdInstanceWrapper)) {
				result = of((SkolemId) variable);
			} else {
				result = variable;
			}
		}

	}

}
