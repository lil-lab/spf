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
package edu.uw.cs.lil.tiny.mr.language.type;

import jregex.Matcher;
import jregex.Pattern;
import edu.uw.cs.utils.assertion.Assert;
import edu.uw.cs.utils.composites.Pair;

/**
 * ComplexType to describe predicates such as "and" and "or" that can take an
 * unlimited number of arguments of the same type.
 * 
 * @author Yoav Artzi
 */
public class RecursiveComplexType extends ComplexType {
	
	public static final int		MIN_NUM_ARGUMENT	= 2;
	private static final long	serialVersionUID	= -9084392421171457642L;
	private final int			minArgs;
	private final boolean		orderSensitive;
	
	RecursiveComplexType(String label, Type domain, Type range,
			boolean orderSensitive, int minArgs) {
		super(label, domain, range);
		this.orderSensitive = orderSensitive;
		this.minArgs = Assert.ifTrue(minArgs, minArgs <= 0);
	}
	
	RecursiveComplexType(String label, Type domain, Type range, Option option) {
		this(label, domain, range, option.isOrderSensitive, option.minNumArgs);
	}
	
	public Type getFinalRange() {
		return super.getRange();
	}
	
	public int getMinArgs() {
		return minArgs;
	}
	
	public Option getOption() {
		return new Option(orderSensitive, minArgs);
	}
	
	@Override
	public Type getRange() {
		return this;
	}
	
	@Override
	public boolean isExtending(Type other) {
		return other != null
				&& (other == this || (other instanceof RecursiveComplexType
						&& (minArgs == ((RecursiveComplexType) other).minArgs)
						&& (orderSensitive == ((RecursiveComplexType) other).orderSensitive)
						&& getDomain().isExtending(other.getDomain()) && getFinalRange()
						.isExtending(
								((RecursiveComplexType) other).getFinalRange())));
	}
	
	@Override
	public boolean isOrderSensitive() {
		return orderSensitive;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	/**
	 * Option that modify {@link ComplexType} to allow recursion of arguments.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Option {
		public static final String		DOMAIN_REPEAT_ORDER_INSENSITIVE	= "*";
		public static final String		DOMAIN_REPEAT_OREDER_SENSITIVE	= "+";
		
		private static final Pattern	STRING_PATTERN					= new Pattern(
																				"({type}.+?)((({order}[*+])(({minargs}\\d+)|()))|())$");
		
		private final boolean			isOrderSensitive;
		private final int				minNumArgs;
		
		public Option(boolean isOrderSensitive, int minNumArgs) {
			this.isOrderSensitive = isOrderSensitive;
			this.minNumArgs = minNumArgs;
		}
		
		public static Pair<String, Option> parse(String string) {
			final Matcher m = STRING_PATTERN.matcher(string);
			if (m.matches()) {
				final String type = m.group("type");
				if (m.isCaptured("order")) {
					final boolean isOrderSensitive = m.group("order").equals(
							"+");
					int minArgs;
					if (m.isCaptured("minargs")) {
						minArgs = Integer.valueOf(m.group("minargs"));
					} else {
						minArgs = 2;
					}
					return Pair.of(type, new Option(isOrderSensitive, minArgs));
				} else {
					return Pair.of(type, null);
				}
			} else {
				throw new IllegalArgumentException("Invalid type string");
			}
		}
		
		@Override
		public String toString() {
			return (isOrderSensitive ? DOMAIN_REPEAT_OREDER_SENSITIVE
					: DOMAIN_REPEAT_ORDER_INSENSITIVE)
					+ (minNumArgs == 2 ? "" : String.valueOf(minNumArgs));
		}
	}
	
}
