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
package edu.cornell.cs.nlp.spf.data.utils;

import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;

/**
 * Simple validator for labeled data items. Compares the hypothesis label to the
 * gold label.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Labeled data item to use for validation.
 * @param <LABEL>
 *            Type of label.
 */
public class LabeledValidator<DI extends ILabeledDataItem<?, LABEL>, LABEL>
		implements IValidator<DI, LABEL> {
	
	@Override
	public boolean isValid(DI dataItem, LABEL label) {
		return dataItem.getLabel().equals(label);
	}
	
	public static class Creator<DI extends ILabeledDataItem<?, LABEL>, LABEL>
			implements IResourceObjectCreator<LabeledValidator<DI, LABEL>> {
		
		private String	type;
		
		public Creator() {
			this("validator.labeled");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public LabeledValidator<DI, LABEL> create(Parameters params,
				IResourceRepository repo) {
			return new LabeledValidator<DI, LABEL>();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), LabeledValidator.class)
					.build();
		}
		
	}
	
}
