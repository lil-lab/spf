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
package edu.uw.cs.lil.tiny.mr.lambda.exec.tabular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.HasFreeVariables;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LambdaWrapped;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class Execution implements ILogicalExpressionVisitor {
	private static ILogger				LOG	= LoggerFactory
													.create(Execution.class);
	
	private final LogicalExpression		exp;
	private final boolean				isIndepedentExec;
	
	private final IExecutionServices	services;
	private final Table					table;
	
	private Execution(IExecutionServices services, Table table,
			LogicalExpression exp, boolean isIndepedentExec) {
		this.services = services;
		this.table = table;
		this.exp = exp;
		this.isIndepedentExec = isIndepedentExec;
	}
	
	public static List<Object> of(final LogicalExpression exp,
			IExecutionServices services) {
		return of(exp, services, false);
	}
	
	/**
	 * @param exp
	 * @param services
	 * @param isIndependentExec
	 *            Allow independent sub expression (contain no free variables)
	 *            to be executed in a new executor. Turning ON this option may
	 *            make execution significantly faster.
	 * @return
	 */
	public static List<Object> of(final LogicalExpression exp,
			IExecutionServices services, boolean isIndependentExec) {
		final Execution visitor = new Execution(services, new Table(), exp,
				isIndependentExec);
		visitor.visit(exp);
		
		if (visitor.table.numRows() != 1) {
			LOG.error("Expected a single row, instead:");
			LOG.error(visitor.table);
			throw new IllegalStateException("More than one row in final table");
		}
		
		final List<Object> denotations = ListUtils.map(visitor.table,
				new ListUtils.Mapper<Map<LogicalExpression, Object>, Object>() {
					
					@Override
					public Object process(Map<LogicalExpression, Object> obj) {
						return obj.get(exp);
					}
				});
		LOG.debug("Denoted: %s", exp);
		LOG.debug("%s", denotations);
		return denotations;
	}
	
	/**
	 * Decomposes a logical expression as a SELECT query.
	 * 
	 * @param exp
	 * @return Pair of queried variables and SELECT body. If not a SELECT query,
	 *         returns null.
	 */
	private static Pair<List<Variable>, LogicalExpression> decomposeLogicalExpressionAsSelect(
			LogicalExpression exp) {
		LogicalExpression currentBody = exp;
		final List<Variable> queryVariables = new LinkedList<Variable>();
		while (currentBody instanceof Lambda) {
			final Lambda lambda = (Lambda) exp;
			if (lambda.getArgument().getType().isComplex()) {
				// Case argument is complex
				return null;
			} else {
				queryVariables.add(lambda.getArgument());
				currentBody = lambda.getBody();
			}
		}
		
		if (currentBody.getType().equals(
				LogicLanguageServices.getTypeRepository().getTruthValueType())) {
			return Pair.of(queryVariables, currentBody);
		} else {
			return null;
		}
	}
	
	@Override
	public void visit(Lambda lambda) {
		// If independent sub-expression (contain no free variables) may be
		// executed independently, try to do so
		if (isIndepedentExec && lambda != exp && !HasFreeVariables.of(lambda)) {
			// Case not the original expression, if it has no free variables,
			// evaluate it separately
			table.augment(lambda, of(lambda, services));
			return;
		}
		
		// Get the queried variables (the ones we are SELECTing on) and the body
		// of the select operation. The type of the logical expression has to be
		// <?,<?,<?...<?,t>..>>>, with all arguments being primitive types and
		// the final body being type 't'. The query variables are all these
		// arguments. First, init the table with these variables (all
		// combinations of assignments to them) and then evaluate the deepest
		// body. If the logical expression is not of this type, we can't execute
		// it.
		final Pair<List<Variable>, LogicalExpression> selectDecomposition = decomposeLogicalExpressionAsSelect(lambda);
		if (selectDecomposition != null) {
			// Case SELECT expression
			final LogicalExpression queryBody = selectDecomposition.second();
			final List<Variable> queryVariables = selectDecomposition.first();
			for (final Variable variable : queryVariables) {
				services.augmentTableWithVariable(variable, table);
			}
			LOG.debug("Lambda SELECT execution: query_variables=%s, body=%s",
					queryVariables, queryBody);
			LOG.debug("augmented table size: %d", table.numRows());
			
			queryBody.accept(this);
			
			// Iterate over all rows. Collect the set of variables, remove body
			// and queried varialbes and cluster them back together. Basically
			// compressing the table back.
			final Map<Map<LogicalExpression, Object>, Set<List<Object>>> compressedRows = new HashMap<Map<LogicalExpression, Object>, Set<List<Object>>>();
			for (final Map<LogicalExpression, Object> row : table) {
				// Create the queried tuple from the row
				final List<Object> queryObjects;
				if (Boolean.TRUE.equals(row.get(queryBody))) {
					// Case a valid assignment of the Lambda body
					queryObjects = new ArrayList<Object>(queryVariables.size());
					for (final Variable var : queryVariables) {
						queryObjects.add(row.get(var));
						// Remove the variable from the row
						services.removingVariable(var, row);
						row.remove(var);
					}
				} else {
					// Case not a valid assignment, this tuple is not included
					// in the set
					queryObjects = null;
					// Clean the row from the variables
					for (final Variable var : queryVariables) {
						// Remove the variable from the row
						services.removingVariable(var, row);
						row.remove(var);
					}
				}
				// Remove the body from the row
				row.remove(queryBody);
				
				// Add the row to the cluster, add the result tuple to the
				// output list
				if (!compressedRows.containsKey(row)) {
					compressedRows.put(row, new HashSet<List<Object>>());
				}
				if (queryObjects != null) {
					// Only add the tuple of values, if it evaluated the body to
					// 'true'
					compressedRows.get(row).add(queryObjects);
				}
			}
			
			// Remove all row from the table, and re-add the compressed rows,
			// each with its set of tuples
			table.clear();
			for (final Map.Entry<Map<LogicalExpression, Object>, Set<List<Object>>> entry : compressedRows
					.entrySet()) {
				entry.getKey().put(lambda, entry.getValue());
				table.addRow(entry.getKey());
			}
			
		} else {
			throw new IllegalArgumentException("invalid lambda: " + lambda);
		}
	}
	
	@Override
	public void visit(Literal literal) {
		LOG.info("Visiting literal: %s", literal);
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
		}
		
		services.augmentTableWithLiteral(literal, table);
		
		// Remove the arguments from the table
		for (final LogicalExpression arg : literal.getArguments()) {
			if (!(arg instanceof Variable)) {
				table.removeColumn(arg);
			}
		}
		
		LOG.debug("Executed literal: %s", literal);
		LOG.debug("table size: %d", table.numRows());
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Case complex constant (a predicate that was simplified into a
		// stand-alone constants, since predicates of literals are never
		// visited), try to execute it
		if (logicalConstant.getType().isComplex()) {
			final LogicalExpression wrapped = LambdaWrapped.of(logicalConstant);
			if (isIndepedentExec) {
				final List<Object> wrappedResult = of(wrapped, services, true);
				table.augment(logicalConstant, wrappedResult);
			} else {
				visit(wrapped);
				// Replace the result in the table, so it will map to this
				// logical constant
				for (final Map<LogicalExpression, Object> row : table) {
					row.put(logicalConstant, row.get(wrapped));
					row.remove(wrapped);
				}
			}
		} else {
			services.augmentTableWithConstant(logicalConstant, table);
		}
		
		LOG.debug("Executed constant: %s", logicalConstant);
		LOG.debug("table size: %d", table.numRows());
		
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		// Nothing to do
	}
}
