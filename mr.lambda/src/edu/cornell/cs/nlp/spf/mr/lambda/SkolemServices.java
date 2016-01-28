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

import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Service class for using generalized skolem terms.
 * 
 * @author Yoav Artzi
 */
public class SkolemServices {
	
	private static SkolemServices	INSTANCE;
	private final LogicalConstant	idPlaceholder;
	private final Type				idType;
	
	private SkolemServices(Type idType, LogicalConstant idPlaceholder) {
		this.idType = idType;
		this.idPlaceholder = idPlaceholder;
	}
	
	public static LogicalConstant getIdPlaceholder() {
		return INSTANCE.idPlaceholder;
	}
	
	public static Type getIDType() {
		return INSTANCE == null ? null : INSTANCE.idType;
	}
	
	public static void setInstance(SkolemServices skolemServices) {
		SkolemServices.INSTANCE = skolemServices;
	}
	
	public static class Builder {
		
		private final LogicalConstant	idPlaceholder;
		private final Type				idType;
		
		public Builder(Type idType, LogicalConstant idPlaceholder) {
			this.idType = idType;
			this.idPlaceholder = idPlaceholder;
		}
		
		public SkolemServices build() {
			return new SkolemServices(idType, idPlaceholder);
		}
		
	}
	
}
