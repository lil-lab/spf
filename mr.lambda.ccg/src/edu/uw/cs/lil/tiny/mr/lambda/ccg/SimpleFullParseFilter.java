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
package edu.uw.cs.lil.tiny.mr.lambda.ccg;

import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsWellTyped;
import edu.uw.cs.utils.filter.IFilter;

public class SimpleFullParseFilter<Y> implements IFilter<Category<Y>> {
	
	private final Set<Syntax>	fullSentenceSyntaxes;
	
	public SimpleFullParseFilter(Set<Syntax> fullSentenceSyntaxes) {
		this.fullSentenceSyntaxes = fullSentenceSyntaxes;
	}
	
	@Override
	public boolean isValid(Category<Y> category) {
		return category.getSem() != null
				&& fullSentenceSyntaxes.contains(category.getSyntax())
				&& IsWellTyped.of((LogicalExpression) category.getSem());
	}
	
}
