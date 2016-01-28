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
package edu.cornell.cs.nlp.spf.parser.ccg.normalform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cornell.cs.nlp.spf.parser.ccg.rules.IArrayRuleNameSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Validates parse steps to enforce normal form constraints. Using NF parsing
 * with approximate inference may lead to losing parses. While NF parsing
 * guarantees that all final categories (logical forms) will be generated, when
 * the chart is pruned this guarantee doesn't hold. However, NF parsing leads to
 * fewer entries in the chart, which in principle is better for exploring the
 * space.
 *
 * @author Yoav Artzi
 */
public class NormalFormValidator implements Serializable {
	public static final ILogger				LOG					= LoggerFactory
																		.create(NormalFormValidator.class);

	private static final long				serialVersionUID	= -3843293772350223393L;

	private final INormalFormConstraint[]	constraints;

	private NormalFormValidator(INormalFormConstraint[] constraints) {
		this.constraints = constraints;
		LOG.info("Init %s :: constraints=%s",
				NormalFormValidator.class.getSimpleName(),
				Arrays.toString(constraints));
	}

	/**
	 * Validates a binary parsing step.
	 *
	 * @param left
	 *            The set of rules that generated the left category argument of
	 *            considered parse step.
	 * @param right
	 *            The set of rules that generated the right category argument of
	 *            considered parse step.
	 * @param consideredRule
	 *            The rule of the considered parse step.
	 */
	public boolean isValid(IArrayRuleNameSet left, IArrayRuleNameSet right,
			RuleName consideredRule) {
		final int length = constraints.length;
		for (int i = 0; i < length; ++i) {
			if (!constraints[i].isValid(left, right, consideredRule)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Validates a unary parsing step.
	 *
	 * @param generatingRules
	 *            The set of rules that generated the category argument of the
	 *            considered unary parse step.
	 * @param consideredRule
	 *            The rule of the considered parse step.
	 */
	public boolean isValid(IArrayRuleNameSet generatingRules,
			RuleName consideredRule) {
		final int length = constraints.length;
		for (int i = 0; i < length; ++i) {
			if (!constraints[i].isValid(generatingRules, consideredRule)) {
				return false;
			}
		}
		return true;
	}

	public static class Builder {

		private final List<INormalFormConstraint>	constraints	= new ArrayList<INormalFormConstraint>();

		public Builder addConstraint(INormalFormConstraint constraint) {
			constraints.add(constraint);
			return this;
		}

		public Builder addConstraints(NormalFormValidator otherValidator) {
			for (final INormalFormConstraint constraint : otherValidator.constraints) {
				constraints.add(constraint);
			}
			return this;
		}

		public NormalFormValidator build() {
			return new NormalFormValidator(
					constraints.toArray(new INormalFormConstraint[constraints
							.size()]));
		}
	}
}
