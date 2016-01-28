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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured rule name. Indicates the direction of the rule, its label and
 * order.
 *
 * @author Yoav Artzi
 */
public class RuleName implements Serializable {

	public static String		RULE_ADD			= "+";
	private static final long	serialVersionUID	= -8734352006518878281L;
	private final Direction		direction;
	private final int			hashCode;

	private final String		label;
	private final int			order;

	private RuleName(String label, Direction direction) {
		this(label, direction, 0);
	}

	protected RuleName(String label) {
		this(label, null);
	}

	protected RuleName(String label, Direction direction, int order) {
		assert label != null;
		this.label = label;
		this.direction = direction;
		this.order = order;
		this.hashCode = calcHashCode();
	}

	public static RuleName create(String label, Direction direction) {
		return new RuleName(label, direction);
	}

	public static RuleName create(String label, Direction direction, int order) {
		return new RuleName(label, direction, order);
	}

	public static String[] splitRuleLabel(RuleName ruleName) {
		return splitRuleLabel(ruleName.getLabel());
	}

	public static String[] splitRuleLabel(String label) {
		return label.split("\\" + RULE_ADD);
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
		final RuleName other = (RuleName) obj;
		if (direction == null) {
			if (other.direction != null) {
				return false;
			}
		} else if (!direction.equals(other.direction)) {
			return false;
		}
		if (!label.equals(other.label)) {
			return false;
		}
		if (order != other.order) {
			return false;
		}
		return true;
	}

	public Direction getDirection() {
		return direction;
	}

	public String getLabel() {
		return label;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public int hashCode() {
		return hashCode;

	}

	public OverloadedRuleName overload(UnaryRuleName unaryRule) {
		return new OverloadedRuleName(unaryRule, this);
	}

	@Override
	public String toString() {
		return (direction == null ? "" : direction.toString()) + label
				+ (order == 0 ? "" : Integer.toString(order));
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (direction == null ? 0 : direction.hashCode());
		result = prime * result + (label == null ? 0 : label.hashCode());
		result = prime * result + order;
		return result;
	}

	public static class Direction implements Serializable {
		public static final Direction				BACKWARD			= new Direction(
																				"<");
		public static final Direction				FORWARD				= new Direction(
																				">");
		private static final long					serialVersionUID	= 1503654147785553576L;
		private static final Map<String, Direction>	STRING_MAPPING;

		private static final List<Direction>		VALUES;
		private final int							hashCode;
		private final String						label;

		private Direction(String label) {
			this.label = label;
			this.hashCode = calcHashCode();
		}

		static {
			final Map<String, Direction> stringMapping = new HashMap<String, Direction>();

			stringMapping.put(FORWARD.toString(), FORWARD);
			stringMapping.put(BACKWARD.toString(), BACKWARD);

			STRING_MAPPING = Collections.unmodifiableMap(stringMapping);
			VALUES = Collections.unmodifiableList(new ArrayList<Direction>(
					stringMapping.values()));
		}

		public static Direction valueOf(String string) {
			return STRING_MAPPING.get(string);
		}

		public static List<Direction> values() {
			return VALUES;
		}

		@Override
		public boolean equals(Object obj) {
			// Singletons, so can compare by instance.
			return obj == this;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return label;
		}

		private int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (label == null ? 0 : label.hashCode());
			return result;
		}

		/**
		 * Resolve to one of the static members.
		 *
		 * @throws ObjectStreamException
		 */
		protected Object readResolve() throws ObjectStreamException {
			return valueOf(toString());
		}

	}

}
