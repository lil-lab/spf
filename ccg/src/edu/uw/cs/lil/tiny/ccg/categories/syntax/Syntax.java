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
package edu.uw.cs.lil.tiny.ccg.categories.syntax;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Syntax symbol.
 * 
 * @author Yoav Artzi
 */
public abstract class Syntax implements Serializable {
	public static final SimpleSyntax				ADJ					= new SimpleSyntax(
																				"ADJ");
	public static final SimpleSyntax				AP					= new SimpleSyntax(
																				"AP");
	public static final SimpleSyntax				C					= new SimpleSyntax(
																				"C");
	public static final SimpleSyntax				DEG					= new SimpleSyntax(
																				"DEG");
	public static final SimpleSyntax				EMPTY				= new SimpleSyntax(
																				"EMPTY");
	
	public static final SimpleSyntax				N					= new SimpleSyntax(
																				"N");
	public static final SimpleSyntax				NP					= new SimpleSyntax(
																				"NP");
	public static final SimpleSyntax				PP					= new SimpleSyntax(
																				"PP");
	
	public static final SimpleSyntax				S					= new SimpleSyntax(
																				"S");
	private static final long						serialVersionUID	= -3852094966016976417L;
	
	private static final Map<String, SimpleSyntax>	STRING_MAPPING;
	
	private static final List<SimpleSyntax>			VALUES;
	
	static {
		final Map<String, SimpleSyntax> stringMapping = new HashMap<String, SimpleSyntax>();
		
		stringMapping.put(AP.toString(), AP);
		stringMapping.put(NP.toString(), NP);
		stringMapping.put(EMPTY.toString(), EMPTY);
		stringMapping.put(PP.toString(), PP);
		stringMapping.put(S.toString(), S);
		stringMapping.put(C.toString(), C);
		stringMapping.put(N.toString(), N);
		stringMapping.put(DEG.toString(), DEG);
		stringMapping.put(ADJ.toString(), ADJ);
		
		STRING_MAPPING = Collections.unmodifiableMap(stringMapping);
		VALUES = Collections.unmodifiableList(new ArrayList<SimpleSyntax>(
				stringMapping.values()));
	}
	
	public static SimpleSyntax valueOf(String string) {
		return STRING_MAPPING.get(string);
	}
	
	public static List<SimpleSyntax> values() {
		return VALUES;
	}
	
	@Override
	public abstract boolean equals(Object obj);
	
	@Override
	public abstract int hashCode();
	
	public abstract int numSlashes();
	
	@Override
	public abstract String toString();
	
	public static class SimpleSyntax extends Syntax {
		private static final long	serialVersionUID	= -4823302344425065888L;
		private final int			hashCode;
		private final String		label;
		
		private SimpleSyntax(String label) {
			this.label = label;
			this.hashCode = calcHashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SimpleSyntax other = (SimpleSyntax) obj;
			if (label == null) {
				if (other.label != null) {
					return false;
				}
			} else if (!label.equals(other.label)) {
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public int numSlashes() {
			return 0;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
		private int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}
		
		/**
		 * Resolve to one of the enumerated static members.
		 * 
		 * @return
		 * @throws ObjectStreamException
		 */
		protected Object readResolve() throws ObjectStreamException {
			return valueOf(toString());
		}
	}
	
}
