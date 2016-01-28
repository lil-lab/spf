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
package edu.cornell.cs.nlp.spf.genlex.ccg.unification.split;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.language.type.RecursiveComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Support code for higher-order unification splitting (application and
 * composition cases).
 *
 * @author Luke Zettlemoyer
 */
public class SplittingServices {
	public static final int	MAX_NUM_SUBS	= 3;

	public static final int	MAX_NUM_VARS	= 2;

	private SplittingServices() {
	}

	/**
	 * Works us all the possible variable orderings of a given set of variables.
	 *
	 * @param input
	 * @return
	 */
	public static List<List<Variable>> allOrders(Set<Variable> input) {
		final List<List<Variable>> result = new LinkedList<List<Variable>>();
		if (input.size() < 2) {
			final List<Variable> copy = new LinkedList<Variable>();
			copy.addAll(input);
			result.add(copy);
			return result;
		}
		final Set<Variable> subSet = new HashSet<Variable>();
		subSet.addAll(input);
		for (final Variable v : input) {
			subSet.remove(v);
			for (final List<Variable> order : allOrders(subSet)) {
				order.add(0, v);
				result.add(order);
			}
			subSet.add(v);
		}
		return result;
	}

	public static LogicalExpression makeAssignment(List<Variable> argVars,
			Variable newVar) {

		if (argVars == null || newVar == null) {
			return null;
		}
		if (argVars.size() == 0) {
			return newVar;
		}

		// Wrapping the list, so we can use it to create a literal. Ugly
		// solution, but welcome to the wonderful world of Java generics.
		return new Literal(newVar,
				argVars.toArray(new LogicalExpression[argVars.size()]));
	}

	/**
	 * Create a function from a given body and variables.
	 *
	 * @param vars
	 *            List of variables to use. The output will take the first
	 *            variable as the first argument and so on.
	 * @param body
	 *            Logical expression to use as function body
	 * @return
	 */
	public static LogicalExpression makeExpression(List<Variable> argVars,
			LogicalExpression body) {
		if (argVars == null || body == null) {
			return null;
		}
		if (argVars.size() == 0) {
			return body;
		}
		LogicalExpression newFunction = body;
		for (int i = argVars.size() - 1; i >= 0; i--) {
			final Variable var = argVars.get(i);
			newFunction = new Lambda(var, newFunction);
		}
		return newFunction;
	}

	public static Syntax typeToSyntax(Type type) {
		if (type instanceof RecursiveComplexType) {
			// Basically something like and:<t*,t>, so we need two arguments, to
			// get something like N|N|N
			final RecursiveComplexType recursiveType = (RecursiveComplexType) type;
			return new ComplexSyntax(
					typeToSyntax(recursiveType.getFinalRange()),
					recurviseArgsToSyntax(recursiveType.getDomain(),
							recursiveType.getMinArgs()), Slash.VERTICAL);
		} else if (type.isComplex()) {
			return new ComplexSyntax(typeToSyntax(type.getRange()),
					typeToSyntax(type.getDomain()), Slash.VERTICAL);
		} else if (type == LogicLanguageServices.getTypeRepository()
				.getTruthValueType()) {
			// Case primitive type.
			// All things of type T have CCG category S
			return Syntax.S;
		} else {
			// Else NP
			return Syntax.NP;
		}
	}

	private static Syntax recurviseArgsToSyntax(Type type, int numArgs) {
		final Syntax baseCategory = typeToSyntax(type);
		Syntax current = baseCategory;
		for (int i = 1; i < numArgs; ++i) {
			current = new ComplexSyntax(baseCategory, current, Slash.VERTICAL);
		}
		return current;
	}

	public static class SplittingPair {
		final Category<LogicalExpression>	left;
		final Category<LogicalExpression>	right;

		public SplittingPair(Category<LogicalExpression> left,
				Category<LogicalExpression> right) {
			this.left = left;
			this.right = right;
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
			final SplittingPair other = (SplittingPair) obj;
			if (left == null) {
				if (other.left != null) {
					return false;
				}
			} else if (!left.equals(other.left)) {
				return false;
			}
			if (right == null) {
				if (other.right != null) {
					return false;
				}
			} else if (!right.equals(other.right)) {
				return false;
			}
			return true;
		}

		public Category<LogicalExpression> getLeft() {
			return left;
		}

		public Category<LogicalExpression> getRight() {
			return right;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (left == null ? 0 : left.hashCode());
			result = prime * result + (right == null ? 0 : right.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "[" + left + ", " + right + "]";
		}
	}
}
