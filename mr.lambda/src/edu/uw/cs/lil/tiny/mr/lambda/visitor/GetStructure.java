package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Replace the base name of constants with an anonymized name. Keep types.
 * 
 * @author Yoav Artzi
 */
public class GetStructure implements ILogicalExpressionVisitor {
	
	private static final String	DEFAULT_ANONNYMOUS_TAG	= "anon";
	private final String		anonnymousTag;
	private LogicalExpression	tempReturn;
	
	public GetStructure(String anonnymousName) {
		this.anonnymousTag = anonnymousName;
	}
	
	public static LogicalExpression of(LogicalExpression exp) {
		return of(exp, DEFAULT_ANONNYMOUS_TAG);
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			String anonnymousName) {
		final GetStructure visitor = new GetStructure(anonnymousName);
		visitor.visit(exp);
		return visitor.tempReturn;
	}
	
	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
		if (lambda.getBody() == tempReturn) {
			tempReturn = lambda;
		} else {
			tempReturn = new Lambda(lambda.getArgument(), tempReturn);
		}
		
	}
	
	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate = tempReturn;
		
		boolean argsChanged = false;
		final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
				literal.getArguments().size());
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
			newArgs.add(tempReturn);
			if (arg != tempReturn) {
				argsChanged = true;
			}
		}
		
		if (argsChanged || newPredicate != literal.getPredicate()) {
			tempReturn = new Literal(newPredicate, argsChanged ? newArgs
					: literal.getArguments());
		} else {
			tempReturn = literal;
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		tempReturn = LogicalConstant.create(
				LogicalConstant.makeName(anonnymousTag,
						logicalConstant.getType()), logicalConstant.getType());
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		tempReturn = variable;
	}
	
}
