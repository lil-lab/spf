package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Forward type raising: X : a => T\(T/X) : \lambda f. f(a).
 * 
 * @author Yoav Artzi
 */
public class BackwardTypeRaising extends AbstractTypeRaising {
	
	public BackwardTypeRaising(IFilter<Syntax> validSyntaxFilter) {
		super(Direction.BACKWARD, validSyntaxFilter);
	}
}
