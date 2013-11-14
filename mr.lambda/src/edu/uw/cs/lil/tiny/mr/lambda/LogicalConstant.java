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

import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda calculus constant. A predicate is also a constant, even the primitive
 * ones, such as 'and:t' and 'not:t'. A constant must have a unique name. The
 * name space is across types as well. Since constants are being re-used, their
 * unique identifier is their name.
 * 
 * @author Yoav Artzi
 */
public class LogicalConstant extends Term {
	public static final ILogger	LOG					= LoggerFactory
															.create(LogicalConstant.class);
	
	private static final String	DYNAMIC_MARKER		= "@";
	
	private static final long	serialVersionUID	= 4418490882304760062L;
	
	private final String		name;
	
	protected LogicalConstant(String name, Type type) {
		super(type);
		this.name = name;
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
	
	public static LogicalConstant parse(String string) {
		return parse(string, LogicLanguageServices.getTypeRepository());
	}
	
	protected static LogicalConstant doParse(String string,
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
	
	protected static LogicalConstant parse(String string,
			TypeRepository typeRepository) {
		return doParse(string, typeRepository);
	}
	
	protected static LogicalExpression parse(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		return parse(string, typeRepository);
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
		return name.split(Term.TYPE_SEPARATOR)[0];
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
			Map<Variable, Variable> variablesMapping) {
		// Do instance comparison, since constants are unique across the system.
		return this == exp;
	}
	
	@Override
	protected boolean equals(LogicalExpression exp,
			Map<Variable, Variable> variablesMapping) {
		// Constants are singletons and re-used, so do instance comparison.
		return this == exp;
	}
	
	/**
	 * Resolves read serialized objects to constants from the repository.
	 * 
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		return create(getName(), getType());
	}
	
}
