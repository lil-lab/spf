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
package edu.cornell.cs.nlp.spf.explat.resources.usage;

/**
 * Single parameter usage. Includes: name, value type, description and example
 * value. All fields are simple strings.
 * 
 * @author Yoav Artzi
 */
public class ParamUsage {
	
	private final String	description;
	
	private final String	name;
	private final String	valueType;
	
	public ParamUsage(String name, String valueType, String description) {
		this.name = name;
		this.valueType = valueType;
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValueType() {
		return valueType;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(name).append(" [").append(valueType)
				.append("]\t").append(description).toString();
	}
	
}
