package edu.uw.cs.lil.tiny.parser.ccg.rules;

/**
 * Unary rule name. Only contains a label. A unary rule has no order or
 * directionality.
 * 
 * @author Yoav Artzi
 */
public class UnaryRuleName extends RuleName {
	private UnaryRuleName(String label) {
		super(label);
	}
	
	public static UnaryRuleName create(String label) {
		return new UnaryRuleName(label);
	}
	
}
