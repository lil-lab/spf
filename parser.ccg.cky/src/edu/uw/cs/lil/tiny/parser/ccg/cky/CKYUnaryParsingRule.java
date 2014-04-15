package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IUnaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

/**
 * A CKY rule wrapping a {@link IUnaryParseRule}.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class CKYUnaryParsingRule<MR> {
	
	private final IUnaryParseRule<MR>	rule;
	
	public CKYUnaryParsingRule(IUnaryParseRule<MR> rule) {
		this.rule = rule;
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]",
				CKYBinaryParsingRule.class.getSimpleName(), rule);
	}
	
	/**
	 * Applies the underlying parse rule to a single cell.
	 */
	protected Collection<ParseRuleResult<MR>> apply(Cell<MR> cell) {
		return rule.apply(cell.getCategory());
	}
	
	/**
	 * @see IUnaryParseRule#isValidArgument(Category)
	 */
	boolean isValidArgument(Category<MR> category) {
		return rule.isValidArgument(category);
	}
}
