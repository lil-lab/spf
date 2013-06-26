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
package edu.uw.cs.lil.tiny.data;

/**
 * Represents a data item that can given loss and pruning information.
 * 
 * @author Yoav Artzi
 * @param <SAMPLE>
 *            Type of the sample.
 * @param <LABEL>
 *            Type of the label.
 */
public interface ILossDataItem<SAMPLE, LABEL> extends IDataItem<SAMPLE> {
	
	/**
	 * Scores a label.
	 * 
	 * @param label
	 * @return
	 */
	double calculateLoss(LABEL label);
	
	/**
	 * Indicates if to prune a proposed label or not.
	 * 
	 * @param y
	 * @return true if to prune the proposed label
	 */
	boolean prune(LABEL y);
	
	/**
	 * A normalized (0 - 1.0) quality measure. The higher the better.
	 * 
	 * @return
	 */
	double quality();
}
