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
package edu.uw.cs.lil.tiny.data.utils;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Simple validator for labeled data items. Compares the hypothesis label to the
 * gold label.
 * 
 * @author Yoav Artzi
 * @param <X>
 * @param <Z>
 */
public class LabeledValidator<X, Z> implements IValidator<IDataItem<X>, Z> {
	private static final ILogger	LOG	= LoggerFactory
												.create(LabeledValidator.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean isValid(IDataItem<X> dataItem, Z label) {
		if (dataItem instanceof ILabeledDataItem) {
			return (((ILabeledDataItem<X, Z>) dataItem).getLabel())
					.equals(label);
		} else {
			LOG.warn("Unable to validate with: %s", dataItem);
			return false;
		}
	}
	
}
