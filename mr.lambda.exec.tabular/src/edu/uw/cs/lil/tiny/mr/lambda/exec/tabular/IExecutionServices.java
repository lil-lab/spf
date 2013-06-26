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

import java.util.Map;

import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

public interface IExecutionServices {
	void augmentTableWithConstant(LogicalConstant constant, Table table);
	
	void augmentTableWithLiteral(Literal literal, Table table);
	
	void augmentTableWithVariable(Variable variable, Table table);
	
	/**
	 * Called just before a variable is removed from the table. Should be used
	 * to update other row values that depend on the variable.
	 * 
	 * @param var
	 * @param row
	 */
	void removingVariable(Variable var, Map<LogicalExpression, Object> row);
}
