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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;

/**
 * Resource ({@link IResourceObjectCreator}) usage object.
 *
 * @author Yoav Artzi
 */
public class ResourceUsage {

	private final String			description;
	private final List<ParamUsage>	paramUsages;
	private final String			resourceClass;
	private final String			resourceName;

	private ResourceUsage(String resourceName, String resourceClass,
			String description, List<ParamUsage> paramUsages) {
		this.resourceName = resourceName;
		this.resourceClass = resourceClass;
		this.description = description;
		this.paramUsages = Collections.unmodifiableList(paramUsages);
	}

	public static Builder builder(ResourceUsage usage) {
		return new Builder(usage);
	}

	public static Builder builder(String resourceName, Class<?> resourceClass) {
		return new Builder(resourceName, resourceClass);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(resourceName).append('\n')
				.append(resourceClass).append('\n');

		if (description != null) {
			sb.append(description).append('\n');
		}

		for (final ParamUsage paramUsage : paramUsages) {
			sb.append('\t').append(paramUsage).append('\n');
		}

		return sb.toString();
	}

	public static class Builder {
		private final List<ParamUsage>	paramUsages			= new LinkedList<ParamUsage>();
		private final String			resourceClass;
		private String					resourceDescription	= null;
		private final String			resourceName;

		public Builder(ResourceUsage usage) {
			this.resourceName = usage.resourceName;
			this.resourceClass = usage.resourceClass;
			this.resourceDescription = usage.description;
			this.paramUsages.addAll(usage.paramUsages);
		}

		public Builder(String resourceName, Class<?> resourceClass) {
			this.resourceName = resourceName;
			this.resourceClass = resourceClass.getName();
		}

		public Builder addParam(ParamUsage paramUsage) {
			this.paramUsages.add(paramUsage);
			return this;
		}

		public Builder addParam(String name, Class<?> valueType,
				String description) {
			this.paramUsages.add(new ParamUsage(name, valueType.getName(),
					description));
			return this;
		}

		public Builder addParam(String name, String valueType,
				String description) {
			this.paramUsages.add(new ParamUsage(name, valueType, description));
			return this;
		}

		public ResourceUsage build() {
			return new ResourceUsage(resourceName, resourceClass,
					resourceDescription, paramUsages);
		}

		public Builder setDescription(String description) {
			this.resourceDescription = description;
			return this;
		}

	}

}
