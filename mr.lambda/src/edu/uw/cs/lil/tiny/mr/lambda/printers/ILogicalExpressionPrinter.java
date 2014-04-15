package edu.uw.cs.lil.tiny.mr.lambda.printers;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * Printer for converting {@link LogicalExpression} to {@link String}.
 * 
 * @author Yoav Artzi
 */
public interface ILogicalExpressionPrinter {
	
	String toString(LogicalExpression exp);
	
}
