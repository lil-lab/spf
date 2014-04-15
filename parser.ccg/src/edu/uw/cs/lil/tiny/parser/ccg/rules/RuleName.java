package edu.uw.cs.lil.tiny.parser.ccg.rules;

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
public class RuleName {
	
	private static String	RULE_ADD	= "+";
	private final Direction	direction;
	private final String	label;
	
	private final int		order;
	
	private RuleName(String label, Direction direction) {
		this(label, direction, 0);
	}
	
	private RuleName(String label, Direction direction, int order) {
		this.label = label;
		this.direction = direction;
		this.order = order;
	}
	
	protected RuleName(String label) {
		this(label, null);
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
	
	public Direction getDirection() {
		return direction;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getOrder() {
		return order;
	}
	
	public RuleName overload(UnaryRuleName unaryRule) {
		return new RuleName(label + RULE_ADD + unaryRule.getLabel(), direction,
				order);
	}
	
	@Override
	public String toString() {
		return (direction == null ? "" : direction.toString()) + label
				+ (order == 0 ? "" : Integer.toString(order));
	}
	
	public static class Direction {
		public static final Direction				BACKWARD	= new Direction(
																		"<");
		public static final Direction				FORWARD		= new Direction(
																		">");
		private static final Map<String, Direction>	STRING_MAPPING;
		
		private static final List<Direction>		VALUES;
		private final String						label;
		
		private Direction(String label) {
			this.label = label;
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
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Direction other = (Direction) obj;
			if (label == null) {
				if (other.label != null) {
					return false;
				}
			} else if (!label.equals(other.label)) {
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
	}
	
}
