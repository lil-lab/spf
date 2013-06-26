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
package edu.uw.cs.lil.tiny.learn.weakp.loss;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;

/**
 * Loss function with a inner scale value.
 * 
 * @author Yoav Artzi
 * @param <Y>
 *            Type of label
 */
abstract public class AbstractScaledLossFunction<Y> implements ILossFunction<Y> {
	private final double	scale;
	
	public AbstractScaledLossFunction(double scale) {
		this.scale = scale;
	}
	
	final public double calculateLoss(IDataItem<Sentence> dataItem, Y label) {
		return scale * doLossCalculation(dataItem, label);
	}
	
	/**
	 * The actual loss calculation.
	 * 
	 * @param dataItem
	 * @param label
	 * @return
	 */
	protected abstract double doLossCalculation(IDataItem<Sentence> dataItem,
			Y label);
}
