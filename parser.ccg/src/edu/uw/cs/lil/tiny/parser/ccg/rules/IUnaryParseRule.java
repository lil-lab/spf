package edu.uw.cs.lil.tiny.parser.ccg.rules;

import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.Category;

/**
 * A unary parse rule. Consumes a single span and modifies it.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public interface IUnaryParseRule<MR> {
	
	/**
	 * Takes a single category and modifies it.
	 */
	Collection<ParseRuleResult<MR>> apply(Category<MR> category);
	
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
	
}
