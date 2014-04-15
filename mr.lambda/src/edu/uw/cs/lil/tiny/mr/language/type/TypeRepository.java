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
package edu.uw.cs.lil.tiny.mr.language.type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.cs.lil.tiny.base.LispReader;
import edu.uw.cs.utils.composites.Pair;

/**
 * This repository includes all types in the system: primitive ones and complex
 * ones (predicates). The type repository is thread safe as far as adding types.
 * Basically, it means that the repository is completely thread safe and can be
 * shared between threads. However, one thread shouldn't count on the other to
 * add types, unless they devise their own synchronizing mechanism.
 * 
 * @author Yoav Artzi
 */
public class TypeRepository {
	private static final Object		ADDING_LOCK				= new Object();
	
	private static final String		ENTITY_TYPE_NAME		= "e";
	
	private static final String		INDEX_TYPE_NAME			= "ind";
	private static final String		TRUTH_VALUE_TYPE_NAME	= "t";
	
	/**
	 * Type for entities. Every type, except truth value type, index type and
	 * functional type, extends entity.
	 */
	private final TermType			entityType;
	
	/**
	 * Type for indexing numbers for arrays.
	 */
	private final TermType			indexType;
	
	private final boolean			lockPrimitives;
	
	/**
	 * Type for truth value.
	 */
	private final TermType			truthValueType;
	
	/**
	 * The types the system is familiar with. There's one instance of each type
	 * going around the system and it's the one store here.
	 */
	private final Map<String, Type>	types					= new ConcurrentHashMap<String, Type>();
	
	public TypeRepository() {
		this(null);
	}
	
	public TypeRepository(File typesFile) {
		this.indexType = new TermType(INDEX_TYPE_NAME);
		this.truthValueType = new TermType(TRUTH_VALUE_TYPE_NAME);
		this.entityType = new TermType(ENTITY_TYPE_NAME);
		addType(indexType);
		addType(entityType);
		addType(truthValueType);
		// Create the type for array of entities. We must do it since the entity
		// is a built in page
		getArrayTypeCreateIfNeeded(entityType);
		
		if (typesFile != null) {
			try {
				// First, strip the comments and prepare a clean LISP string to
				// parse
				final BufferedReader reader = new BufferedReader(
						new FileReader(typesFile));
				final StringBuilder strippedFile = new StringBuilder();
				try {
					String line = null;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						line = line.split("\\s*//")[0];
						if (!line.equals("")) {
							strippedFile.append(line).append(" ");
						}
					}
				} finally {
					reader.close();
				}
				
				// Get all the types and parse them
				final LispReader lispReader = new LispReader(new StringReader(
						strippedFile.toString()));
				while (lispReader.hasNext()) {
					addType(createTypeFromString(lispReader.next()));
				}
				
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
			
		}
		
		this.lockPrimitives = true;
		
	}
	
	/**
	 * Generalize the given type as much as possible, by climbing up the type
	 * hierarchy.
	 * 
	 * @param type
	 * @return
	 */
	public Type generalizeType(Type type) {
		if (type.isComplex()) {
			final boolean recursiveDomain = type instanceof RecursiveComplexType;
			final RecursiveComplexType.Option option = recursiveDomain ? ((RecursiveComplexType) type)
					.getOption() : null;
			return getTypeCreateIfNeeded(
					generalizeType(recursiveDomain ? ((RecursiveComplexType) type).getFinalRange()
							: type.getRange()),
					generalizeType(type.getDomain()), option);
		} else if (type instanceof TermType) {
			TermType currentType = (TermType) type;
			TermType superType;
			while ((superType = currentType.getParent()) != null) {
				currentType = superType;
			}
			return currentType;
		}
		if (type.isArray()) {
			return getArrayTypeCreateIfNeeded(((ArrayType) type).getBaseType());
		} else {
			throw new RuntimeException("Unhandled Type type: "
					+ type.getClass().getCanonicalName());
		}
	}
	
	public ArrayType getArrayTypeCreateIfNeeded(Type baseType) {
		return (ArrayType) getTypeCreateIfNeeded(baseType.getName()
				+ ArrayType.ARRAY_SUFFIX);
	}
	
	public Type getEntityType() {
		return entityType;
	}
	
	public ComplexType getIndexPredicateTypeForArray(ArrayType arrayType) {
		final Type baseType = arrayType.getBaseType();
		return getTypeCreateIfNeeded(
				getTypeCreateIfNeeded(baseType, indexType), arrayType);
	}
	
	public Type getIndexType() {
		return indexType;
	}
	
	public ComplexType getSubPredicateTypeForArray(ArrayType arrayType) {
		return getTypeCreateIfNeeded(
				getTypeCreateIfNeeded(arrayType, indexType), arrayType);
	}
	
	public Type getTruthValueType() {
		return truthValueType;
	}
	
	public Type getType(String name) {
		return types.get(name);
	}
	
	/**
	 * Fetches a type from the repository. If it doesn't exist and it's a
	 * complex type or an array type, creates it according to the label.
	 * 
	 * @param label
	 * @return null if type doesn't exist and wasn't created, otherwise returns
	 *         the type object
	 */
	public Type getTypeCreateIfNeeded(String label) {
		final Type existingType = getType(label);
		if (existingType == null) {
			// Case doesn't exist
			if (label.startsWith(ComplexType.COMPLEX_TYPE_OPEN_PAREN_STR)
					&& label.endsWith(ComplexType.COMPLEX_TYPE_CLOSE_PAREN_STR)) {
				// Case complex type
				// Case the type doesn't exist and it's a complex type, so
				// create it
				return addType(createComplexTypeFromString(label));
			} else if (label.endsWith(ArrayType.ARRAY_SUFFIX)) {
				// Case array
				return addType(createArrayTypeFromString(label));
			}
		}
		return existingType;
	}
	
	public ComplexType getTypeCreateIfNeeded(Type range, Type domain) {
		return getTypeCreateIfNeeded(range, domain, null);
	}
	
	public ComplexType getTypeCreateIfNeeded(Type range, Type domain,
			RecursiveComplexType.Option option) {
		return (ComplexType) getTypeCreateIfNeeded(ComplexType.composeString(
				range, domain, option));
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		for (final Map.Entry<String, Type> entry : types.entrySet()) {
			ret.append(entry.getKey());
			ret.append("\t::\t");
			ret.append(entry.getValue().toString());
			ret.append('\n');
		}
		return ret.toString();
	}
	
	/**
	 * Adds a type to the repository. If the type is an array, will make sure
	 * the access function type exists, if not will add them.
	 * 
	 * @param type
	 * @return The type added. If the type already exists, will log an error and
	 *         return the existing type object.
	 */
	private Type addType(Type type) {
		if (lockPrimitives && !type.isArray() && !type.isComplex()) {
			throw new RuntimeException("Primitive types adding is disabled: "
					+ type);
		}
		synchronized (ADDING_LOCK) {
			if (types.containsKey(type.getName())) {
				// It's that due to a race condition, one thread added this.
				// This is the only place that is locked. To handle this case
				// properly, we just return the type.
				return getType(type.getName());
			}
			types.put(type.getName(), type);
			if (type.isArray()) {
				// Case we added an array, we need to add its access function as
				// well
				createAndAddArrayAccessTypes((ArrayType) type);
			}
			return type;
		}
	}
	
	private void createAndAddArrayAccessTypes(ArrayType arrayType) {
		// Array index access function type
		getIndexPredicateTypeForArray(arrayType);
		// Array sub function type
		getSubPredicateTypeForArray(arrayType);
	}
	
	/**
	 * Creates an array type
	 * 
	 * @param string
	 *            string of the form x[], where x is a type that is created if
	 *            needed.
	 * @return
	 */
	private Type createArrayTypeFromString(String string) {
		return new ArrayType(string, getTypeCreateIfNeeded(string.substring(0,
				string.length() - ArrayType.ARRAY_SUFFIX.length())), entityType);
	}
	
	private ComplexType createComplexTypeFromString(String string) {
		// Case complex functional type
		final String innerString = string.substring(1, string.length() - 1)
				.trim();
		int i = 0;
		final StringBuilder domainStringBuilder = new StringBuilder();
		char c;
		int parenthesisCounter = 0;
		while (i < innerString.length()
				&& !((c = innerString.charAt(i)) == ComplexType.COMPLEX_TYPE_SEP && parenthesisCounter == 0)) {
			++i;
			domainStringBuilder.append(c);
			if (c == ComplexType.COMPLEX_TYPE_OPEN_PAREN) {
				++parenthesisCounter;
			} else if (c == ComplexType.COMPLEX_TYPE_CLOSE_PAREN) {
				--parenthesisCounter;
			}
		}
		++i;
		final String rangeString = innerString.substring(i).trim();
		final String domainString = domainStringBuilder.toString().trim();
		
		// Check if the domain indicates to a RecursiveComplexType, and if so
		// trim the indication to parse it and raise a flag
		final Pair<String, RecursiveComplexType.Option> prefixOption = RecursiveComplexType.Option
				.parse(domainString);
		final RecursiveComplexType.Option option = prefixOption.second();
		final String domainStringTrimmed = prefixOption.first();
		
		final Type domain = getTypeCreateIfNeeded(domainStringTrimmed);
		final Type range = getTypeCreateIfNeeded(rangeString);
		
		return ComplexType.create(string, domain, range, option);
	}
	
	private Type createTermTypeFromString(String string) {
		// Case we have a complex LISP expression
		final LispReader lispReader = new LispReader(new StringReader(string));
		
		// Label (the name of the type)
		final String label = lispReader.next();
		// The parent type
		final String parentTypeString = lispReader.next();
		final Type parentType = getType(parentTypeString);
		if (parentType instanceof TermType) {
			return new TermType(label, (TermType) parentType);
		} else {
			throw new IllegalArgumentException(
					String.format(
							"Parent (%s) of primitive type (%s) must be a primitive type",
							parentType, label));
		}
	}
	
	private Type createTypeFromString(String string) {
		if (string.endsWith(ArrayType.ARRAY_SUFFIX)) {
			// Array type
			return createArrayTypeFromString(string);
		} else if (string.startsWith("(")) {
			// Term type with inheritance
			return createTermTypeFromString(string);
		} else if (string.startsWith(ComplexType.COMPLEX_TYPE_OPEN_PAREN_STR)
				&& string.endsWith(ComplexType.COMPLEX_TYPE_CLOSE_PAREN_STR)) {
			// Complex type
			return createComplexTypeFromString(string);
		} else {
			// Case a simple primitive type, with no declared parent, so its
			// parent is the entity type
			return new TermType(string);
		}
	}
}
