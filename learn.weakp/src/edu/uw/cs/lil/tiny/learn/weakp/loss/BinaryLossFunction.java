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
 * Scaled binary loss function with a threshold.
 * 
 * @author Yoav Artzi
 */
public class BinaryLossFunction<Y> extends AbstractScaledLossFunction<Y> {
	
	private final ILossFunction<Y>	lossFunction;
	private final double			threshold;
	
	public BinaryLossFunction(double scale, ILossFunction<Y> lossFunction,
			double threshold) {
		super(scale);
		this.lossFunction = lossFunction;
		this.threshold = threshold;
	}
	
	@Override
	protected double doLossCalculation(IDataItem<Sentence> dataItem, Y label) {
		return lossFunction.calculateLoss(dataItem, label) >= threshold ? 1.0
				: 0.0;
	}
}
