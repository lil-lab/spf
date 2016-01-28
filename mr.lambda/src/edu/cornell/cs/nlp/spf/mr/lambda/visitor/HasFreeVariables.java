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

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemId;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;

/**
 * Service to test if a logical expression has free variables with an option to
 * ignore {@link SkolemId}s.
 *
 * @author Yoav Artzi
 */
public class HasFreeVariables {

	public static boolean of(LogicalExpression exp) {
		return of(exp, false);
	}

	public static boolean of(LogicalExpression exp, boolean ignoreSkolemIds) {
		if (ignoreSkolemIds && exp.numFreeVariables() != 0) {
			for (final Variable variable : exp.getFreeVariables()) {
				if (!(variable instanceof SkolemId)) {
					return true;
				}
			}
			return false;
		} else {
			return exp.numFreeVariables() != 0;
		}

	}
}
