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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import java.util.HashMap;
import java.util.Map;

import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Verify typing consistency across the logical form.
 *
 * @author Yoav Artzi
 */
public class IsTypeConsistent implements ILogicalExpressionVisitor {
	public static final ILogger			LOG				= LoggerFactory
																.create(IsTypeConsistent.class);

	private String						message			= null;
	/**
	 * Usually we don't see many variables, so initializing this map to be
	 * relatively small.
	 */
	private final Map<Variable, Type>	variableTypes	= new HashMap<Variable, Type>(
																6);
	private boolean						wellTyped		= true;

	private IsTypeConsistent() {
		// Usage only through static 'of' method.
	}

	public static boolean of(LogicalExpression exp) {
		final IsTypeConsistent visitor = new IsTypeConsistent();
		visitor.visit(exp);
		return visitor.wellTyped;
	}

	public static Pair<Boolean, String> ofVerbose(LogicalExpression exp) {
		final IsTypeConsistent visitor = new IsTypeConsistent();
		visitor.visit(exp);
		return Pair.of(visitor.wellTyped, visitor.message);
	}

	@Override
	public void visit(Lambda lambda) {
		// Record this variable to test its references.
		variableTypes.put(lambda.getArgument(), lambda.getArgument().getType());
		// Visit the body.
		lambda.getBody().accept(this);
		// Remove the variable from the mapping, since we are leaving its scope.
		variableTypes.remove(lambda.getArgument());
	}

	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		// Check the arguments match the type of the function.
		final int len = literal.numArgs();
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);

			// Visit the argument.
			arg.accept(this);

			// Match the type of the argument with the signature type.
			final Type signatureType = literal.getArgSignature(i);
			wellTyped = wellTyped && verifyLiteralArgTyping(arg, signatureType);

			if (!wellTyped) {
				LOG.debug(
						"Literal %s is not well-typed. Mismatch between signature type %s to argument %s.",
						literal, signatureType, arg);
				return;
			}
		}
	}

	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do.
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	@Override
	public void visit(Variable variable) {
		// Nothing to do
	}

	/**
	 * Verify the argument type against the signature type.
	 */
	private boolean verifyLiteralArgTyping(LogicalExpression arg,
			Type signatureType) {
		if (arg instanceof Variable) {
			// Case variable: check according to historical type and allow more
			// flexibility.
			return verifyVariableType((Variable) arg, signatureType);
		} else {
			// If the signature expects an array, the argument must be an array.
			// The relation between the signature type and the argument type
			// should be along the inheritance relation, but can be in either
			// direction.
			final boolean literalWellTyped = signatureType.isArray() == arg
					.getType().isArray()
					&& arg.getType().isExtendingOrExtendedBy(signatureType);
			if (!literalWellTyped) {
				message = "Array argument expected, or provided array argument doesn't extend signature array type";
			}
			return literalWellTyped;
		}
	}

	/**
	 * Verifies consistency between a variable and its usage in a literal. If
	 * the variable's historic type is unknown, it's a free variable -- treat it
	 * like a constant. Therefore, this method shouldn't be called when
	 * encountering the variables definition, but only its usage.
	 */
	private boolean verifyVariableType(Variable variable, Type signatureType) {
		final Type historicaltype = variableTypes.get(variable);
		if (historicaltype == null) {
			// Case not historical type, treat like a constant -- check it's
			// extending or extended by. Also, add to variableTypes. This
			// variable will never be removed from types, since its scoping is
			// global.
			variableTypes.put(variable, variable.getType());
			final boolean goodArg = variable.getType().isExtendingOrExtendedBy(
					signatureType);
			if (!goodArg) {
				message = "Argument type is incompatible with signature type (not extending or extended by signature type)";
			}
			return goodArg;
		} else {
			if (signatureType.isExtending(historicaltype)) {
				// Case the current signature type is a narrower instance of the
				// historical type, so remember it and return 'true'.
				variableTypes.put(variable, signatureType);
				return true;
			} else {
				// Return 'true' if the signature type is a narrower instance of
				// the historical type, so just return 'true'.
				final boolean extendingHistorical = historicaltype
						.isExtending(signatureType);
				if (!extendingHistorical) {
					message = "Mismatch between different uses of the same variable (e.g., one instance casts a specific type that another doesn't extend)";
				}
				return extendingHistorical;
			}
		}
	}

}
