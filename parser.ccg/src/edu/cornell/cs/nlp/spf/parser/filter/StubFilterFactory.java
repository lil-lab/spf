/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.parser.filter;

import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.utils.function.PredicateUtils;

/**
 * Stub filter factory. Generated filters always return 'true'.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 * @param <O>
 *            Object to create the filter from.
 */
public class StubFilterFactory<O, MR> implements IParsingFilterFactory<O, MR> {

	private static final long serialVersionUID = 4085592796616434835L;

	@Override
	public Predicate<ParsingOp<MR>> create(O object) {
		return PredicateUtils.alwaysTrue();
	}

}
