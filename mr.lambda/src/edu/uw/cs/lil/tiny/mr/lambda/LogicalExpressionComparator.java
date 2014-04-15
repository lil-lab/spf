package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.HashMap;

public class LogicalExpressionComparator implements
		ILogicalExpressionComparator {
	
	@Override
	public boolean compare(LogicalExpression o1, LogicalExpression o2) {
		return o1.doEquals(o2,
				new HashMap<LogicalExpression, LogicalExpression>());
	}
	
}
