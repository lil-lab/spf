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
package edu.cornell.cs.nlp.spf.mr.lambda;

import java.util.ArrayList;
import java.util.List;

import jregex.Pattern;
import jregex.Replacer;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.LambdaWrapped;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Utility class to read logical expressions from strings.
 *
 * @author Yoav Artzi
 */
public class LogicalExpressionReader {
	public static LogicalExpressionReader						INSTANCE				= new LogicalExpressionReader();
	public static final ILogger									LOG						= LoggerFactory
																								.create(LogicalExpressionReader.class);
	private static final Replacer								WHITE_SPACE_REPLACER	= new Replacer(
																								new Pattern(
																										"\\s+"),
																								" ");

	private final List<IReader<? extends LogicalExpression>>	readers					= new ArrayList<IReader<? extends LogicalExpression>>();

	private LogicalExpressionReader() {
	}

	static {
		// Register readers for all the basic types.
		register(new Lambda.Reader());
		register(new Literal.Reader());
		register(new Variable.Reader());
		register(new LogicalConstant.Reader());
		register(new SkolemId.Reader());
	}

	/**
	 * Read a logical expression from a LISP formatted string.
	 */
	public static LogicalExpression from(String string) {
		return INSTANCE.read(string);
	}

	public static void register(IReader<? extends LogicalExpression> reader) {
		INSTANCE.readers.add(reader);
	}

	public static void reset() {
		INSTANCE = new LogicalExpressionReader();
	}

	public static void setInstance(LogicalExpressionReader reader) {
		LogicalExpressionReader.INSTANCE = reader;
	}

	/**
	 * Read a logical expression from a LISP formatted string.
	 */
	public LogicalExpression read(String string) {
		return read(string, LogicLanguageServices.getTypeRepository(),
				LogicLanguageServices.getTypeComparator());

	}

	/** {@see #read(String)} */
	private LogicalExpression read(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		// Flatten the string. Replace all white space sequences with a single
		// space.
		final String flatString = WHITE_SPACE_REPLACER.replace(string);
		try {
			return LambdaWrapped.of(read(flatString,
					new ScopeMapping<String, LogicalExpression>(),
					typeRepository, typeComparator));
		} catch (final RuntimeException e) {
			LOG.error("Logical expression syntax error: %s", flatString);
			throw e;
		}
	}

	/**
	 * Read a logical expression from a string.
	 *
	 * @param string
	 *            LISP formatted string.
	 * @param mapping
	 *            Mapping of labels to logical expressions created during
	 *            parsing (for example, so we could re-use variables).
	 */
	protected LogicalExpression read(String string,
			ScopeMapping<String, LogicalExpression> mapping,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		for (final IReader<? extends LogicalExpression> reader : readers) {
			if (reader.test(string)) {
				return reader.read(string, mapping, typeRepository,
						typeComparator, this);
			}
		}
		throw new IllegalArgumentException(
				"Invalid logical expression syntax: " + string);
	}

	public static interface IReader<LOGEXP extends LogicalExpression> extends
			IFilter<String> {

		LOGEXP read(String string,
				ScopeMapping<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader);

	}

}
