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
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.composites.Pair;
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
	static private final Map<String, LogicalConstant>	INSTANCE_REPOSITORY	= new ConcurrentHashMap<String, LogicalConstant>();
	
	private static final ILogger						LOG					= LoggerFactory
																					.create(LogicalConstant.class);
	
	private static final long							serialVersionUID	= 4418490882304760062L;
	
	private final String								name;
	
	private LogicalConstant(String name, Type type) {
		super(type);
		this.name = name;
	}
	
	static public LogicalConstant create(String name, Type type) {
		return create(name, type, false);
	}
	
	static public LogicalConstant create(String name, Type type,
			boolean lockOntology) {
		final LogicalConstant instance = INSTANCE_REPOSITORY.get(name);
		if (instance == null) {
			if (lockOntology) {
				throw new LogicalExpressionRuntimeException(
						String.format(
								"Ontology is locked, can't create logical constant %s with type %s",
								name, type));
			}
			final LogicalConstant newConstant = new LogicalConstant(name, type);
			return addToRepository(name, newConstant);
		} else {
			return instance;
		}
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
	
	/**
	 * Given a constant, will split its name
	 * 
	 * @param constant
	 * @param typeRepository
	 *            Type repository to get the type information from
	 * @return Pair of (name, type)
	 */
	public static Pair<String, Type> splitName(LogicalConstant constant) {
		final String[] split = constant.name.split(Term.TYPE_SEPARATOR);
		return Pair.of(split[0], constant.getType());
	}
	
	static private LogicalConstant addToRepository(String name,
			LogicalConstant constant) {
		// Try to get from repository inside lock to handle race conditions
		synchronized (INSTANCE_REPOSITORY) {
			if (INSTANCE_REPOSITORY.containsKey(name)) {
				return INSTANCE_REPOSITORY.get(name);
			} else {
				INSTANCE_REPOSITORY.put(name, constant);
				return constant;
			}
		}
	}
	
	protected static LogicalConstant doParse(String string,
			TypeRepository typeRepository, boolean lockOntology) {
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
			return create(string, type, lockOntology);
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
	
	@Override
	public boolean equals(Object obj) {
		// Do instance comparison, since constants are unique across the system.
		return this == obj;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected boolean equals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		// Constants are singletons and re-used
		return this == obj;
	}
	
	/**
	 * Resolves read serialized objects to constants from the repository.
	 * 
	 * @return
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		final LogicalConstant existing = INSTANCE_REPOSITORY.get(name);
		if (existing == null) {
			return addToRepository(name, this);
		} else {
			return existing;
		}
	}
	
}
