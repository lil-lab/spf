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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsTypeConsistent;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName.Direction;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;

/**
 * A rule for first doing a type raising of the form:
 * <ul>
 * X -> S/(S/X)
 * </ul>
 * for the left input. This new category is then used in an application of the
 * form
 * <ul>
 * <li>S/(S/X) (S/X) => S</li>
 * </ul>
 * We do this form of just in time type raising to avoid computing the left half
 * when it will not combine with anything on the right.
 *
 * @author Luke Zettlemoyer
 */
public class ForwardTopicalizedApplication extends
		AbstractApplication<LogicalExpression> {
	private static final String	RULE_LABEL			= "tapply";
	private static final long	serialVersionUID	= -5182583499352749772L;

	public ForwardTopicalizedApplication(
			ICategoryServices<LogicalExpression> categoryServices) {
		super(RULE_LABEL, Direction.FORWARD, categoryServices);
	}

	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> left,
			Category<LogicalExpression> right, SentenceSpan span) {

		if (!(right instanceof ComplexCategory<?>)) {
			return null;
		}
		// make sure the right is a complex category, so we have some chance of
		// doing the composition
		final ComplexCategory<LogicalExpression> rightComp = (ComplexCategory<LogicalExpression>) right;

		// make sure right side is a forward slash
		if (!rightComp.hasSlash(Slash.FORWARD)) {
			return null;
		}

		// make sure left half of the right category is an S
		if (!rightComp.getSyntax().getLeft().equals(Syntax.S)) {
			return null;
		}

		// make sure the Xs match
		if (!rightComp.getSyntax().getRight().equals(left.getSyntax())) {
			return null;
		}

		// it all matches!!! make new category and do composition!!!
		// first, make the S/(S/X) including the new logical expression
		final ComplexSyntax sfx = new ComplexSyntax(Syntax.S, left.getSyntax(),
				Slash.FORWARD);
		final ComplexSyntax newSyntax = new ComplexSyntax(Syntax.S, sfx,
				Slash.FORWARD);

		final Variable newVar = new Variable(right.getSemantics().getType());
		final LogicalExpression[] args = new LogicalExpression[1];
		args[0] = left.getSemantics();
		final Literal application = new Literal(newVar, args);
		final Lambda newSem = new Lambda(newVar, application);
		if (!IsTypeConsistent.of(newSem)) {
			return null;
		}

		final ComplexCategory<LogicalExpression> newLeft = new ComplexCategory<LogicalExpression>(
				newSyntax, newSem);
		return doApplication(newLeft, right, false);
	}
}
