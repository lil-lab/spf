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

import edu.cornell.cs.nlp.spf.data.IDataItem;

/**
 * Validates a label using the data item.
 * 
 * @author Yoav Artzi
 * @param <DI>
 *            Type of data item
 * @param <LABEL>
 *            Type of label
 */
public interface IValidator<DI extends IDataItem<?>, LABEL> {
	
	boolean isValid(DI dataItem, LABEL label);
	
}
