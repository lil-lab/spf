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
package edu.uw.cs.lil.tiny.mr.lambda;

import java.io.ObjectStreamException;
import java.util.Map;

import jregex.Pattern;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpressionReader.IReader;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda calculus constant. A constant must have a unique name that matches the
 * regular expression [a-z0-9A-Z_]+:type_name. The marker "@" can be used as a
 * prefix to denote a dynamic constant.
 * 
 * @author Yoav Artzi
 */
public class LogicalConstant extends Term {
	public static final ILogger	LOG						= LoggerFactory
																.create(LogicalConstant.class);
	
	public static final Pattern	REGEXP_NAME_PATTERN;
	private static final String	DYNAMIC_MARKER			= "@";
	private static final String	ILLEGAL_CHARS			= "(),:#";
	private static final String	ILLEGAL_PREFIX_CHARS	= ILLEGAL_CHARS + "!$@";
	
	private static final long	serialVersionUID		= 4418490882304760062L;
	
	private final String		name;
	
	protected LogicalConstant(String name, Type type) {
		super(type);
		this.name = name;
	}
	
	static {
		REGEXP_NAME_PATTERN = new Pattern("(?:" + DYNAMIC_MARKER + "[^"
				+ ILLEGAL_CHARS + "]+)|(?:[^" + ILLEGAL_PREFIX_CHARS + "][^"
				+ ILLEGAL_CHARS + "]*)");
	}
	
	public static LogicalConstant create(String name, Type type) {
		return create(name, type, false);
	}
	
	public static LogicalConstant create(String name, Type type, boolean dynamic) {
		// Strip the dynamic marker if present.
		if (name.startsWith(DYNAMIC_MARKER)) {
			name = name.substring(DYNAMIC_MARKER.length());
			dynamic = true;
		}
		
		if (LogicLanguageServices.getOntology() == null) {
			return new LogicalConstant(name, type);
		} else {
			return LogicLanguageServices.getOntology().getOrAdd(
					new LogicalConstant(name, type), dynamic);
		}
	}
	
	public static LogicalConstant createDynamic(String name, Type type) {
		return create(name, type, true);
	}
	
	public static String escapeString(String string) {
		final StringBuilder output = new StringBuilder();
		boolean first = true;
		for (final char c : string.toCharArray()) {
			if (DYNAMIC_MARKER.indexOf(c) >= 0) {
				if ((first && string.length() > 1) || !first) {
					output.append(c);
				} else {
					output.append("_I" + (int) c + "_");
				}
			} else if (first && ILLEGAL_PREFIX_CHARS.indexOf(c) >= 0) {
				output.append("_I" + (int) c + "_");
			} else if (ILLEGAL_CHARS.indexOf(c) >= 0) {
				output.append("_I" + (int) c + "_");
			} else if (Character.isWhitespace(c)) {
				output.append("_I" + (int) c + "_");
			} else {
				output.append(c);
			}
			first = false;
		}
		return output.toString();
	}
	
	public static boolean isValidName(String name) {
		final String[] split = name.split(":", 2);
		return REGEXP_NAME_PATTERN.matches(split[0])
				&& LogicLanguageServices.getTypeRepository()
						.getTypeCreateIfNeeded(split[1]) != null;
	}
	
	/**
	 * Given the constant's short name (the part before the type) and the type,
	 * will generate a complete and valid constant name.
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	public static String makeName(String name, Type type) {
		return name + Term.TYPE_SEPARATOR + type;
	}
	
	public static LogicalConstant read(String string) {
		return read(string, LogicLanguageServices.getTypeRepository());
	}
	
	protected static LogicalConstant read(String string,
			TypeRepository typeRepository) {
		try {
			final String[] split = string.split(Term.TYPE_SEPARATOR);
			if (split.length != 2) {
				throw new LogicalExpressionRuntimeException(
						"Constant sytax error: " + string);
			}
			
			// The type is the part of the string after the colon
			Type type = typeRepository.getType(split[1]);
			
			if (type == null) {
				// Try to create the type if not found, if this is a primitive
				// type
				// that is unknown, the type repository will return null
				type = typeRepository.getTypeCreateIfNeeded(split[1]);
			}
			
			if (type == null) {
				// If we still fail, the type is unknown
				throw new LogicalExpressionRuntimeException(String.format(
						"Unknown type for: %s", string));
			}
			return create(string, type);
		} catch (final RuntimeException e) {
			LOG.error("Logical constant syntax error: %s", string);
			throw e;
		}
	}
	
	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = super.calcHashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	/**
	 * Override {@link Term#equals(Object)} and
	 * {@link LogicalExpression#equals(Object)} to do simple instance comparison
	 * or more heavy comparison, depending on the presence of an ontology.
	 */
	@Override
	public boolean equals(Object obj) {
		if (LogicLanguageServices.getOntology() == null) {
			return obj instanceof LogicalConstant
					&& doEquals((LogicalConstant) obj);
		} else {
			// Do instance comparison, since constants are unique across the
			// system.
			return this == obj;
		}
	}
	
	/**
	 * Given a constant with the name boo:e, will return 'boo'. Meaning, returns
	 * the base name without the typing suffix or the decoration prefix.
	 */
	public String getBaseName() {
		return name.substring(0, name.length() - getType().getName().length()
				- TYPE_SEPARATOR.length());
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Checks equality based on the content of the logical constant (name and
	 * type).
	 * 
	 * @param exp
	 *            Logical constant to compare.
	 */
	protected boolean doEquals(LogicalConstant exp) {
		if (this == exp) {
			return true;
		}
		if (!super.doEquals(exp)) {
			return false;
		}
		if (name == null) {
			if (exp.name != null) {
				return false;
			}
		} else if (!name.equals(exp.name)) {
			return false;
		}
		return true;
	}
	
	@Override
	protected boolean doEquals(LogicalExpression exp,
			Map<LogicalExpression, LogicalExpression> mapping) {
		// Variable mapping is irrelevant for constants mapping.
		return equals(exp);
	}
	
	@Override
	protected boolean equals(LogicalExpression exp,
			Map<LogicalExpression, LogicalExpression> mapping) {
		// Variable mapping is irrelevant for constants mapping.
		return equals(exp);
	}
	
	/**
	 * Resolves read serialized objects to constants from the repository.
	 * 
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		return create(getName(), getType());
	}
	
	public static class Reader implements IReader<LogicalConstant> {
		
		@Override
		public boolean isValid(String string) {
			return isValidName(string);
		}
		
		@Override
		public LogicalConstant read(String string,
				Map<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader) {
			return LogicalConstant.read(string, typeRepository);
		}
		
	}
	
}
