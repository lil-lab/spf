package edu.uw.cs.lil.tiny.parser.ccg.rules;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;

/**
 * A type raising rule that can be restricted given the category it will be
 * combined with later. Type raising rules are very similar to unary rules, but
 * are integrate into binary rules to avoid creating all potential results.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface ITypeRaisingRule<MR, T> {
	
	public static String	RULE_LABEL	= "T";
	
	/**
	 * Takes a single category and modifies it: X : a => T\(T/X) : \lambda f.
	 * f(a).
	 * 
	 * @param innerAgument
	 *            The syntactic category of the inner argument (X above).
	 * @param finalResult
	 *            The syntactic category of the final result (T above).
	 * @param finalResultSemanticType
	 *            The semantic type of the final result.
	 */
	ParseRuleResult<MR> apply(Category<MR> category, Syntax innerAgument,
			Syntax finalResult, T finalResultSemanticType);
	
	@Override
	boolean equals(Object obj);
	
	RuleName getName();
	
	@Override
	int hashCode();
	
	/**
	 * A quick test to check if the rule may apply to this category. This test
	 * is required to be efficient and return 'true' for all categories the rule
	 * may apply for (i.e., !{@link #isValidArgument(Category)} \implies
	 * {@link #apply(Category, boolean)}.isEmpty()). Naively, this method can
	 * return {@link #apply(Category, boolean)}.isEmpty(). However, while
	 * accurate, this is not efficient.
	 */
	boolean isValidArgument(Category<MR> category);
	
	public static class TypeRaisingNameServices {
		
		public static UnaryRuleName createRuleName(Direction direction) {
			return UnaryRuleName.create(direction.toString() + RULE_LABEL);
		}
		
		public static Direction getDirection(RuleName ruleName) {
			final String label = ruleName.getLabel();
			return Direction.valueOf(label.substring(0, label.length()
					- RULE_LABEL.length()));
		}
		
		public static boolean isTypeRaising(RuleName ruleName) {
			final String label = ruleName.getLabel();
			return ruleName instanceof UnaryRuleName
					&& label.endsWith(RULE_LABEL)
					&& Direction.valueOf(label.substring(0, label.length()
							- RULE_LABEL.length())) != null;
		}
		
	}
	
}
