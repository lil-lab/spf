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

import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;

/**
 * Loss function composed of a series of loss functions.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class CompositeLossFunction<Y> extends AbstractScaledLossFunction<Y> {
	
	private final List<ILossFunction<Y>>	lossFunctions;
	
	public CompositeLossFunction(double scale,
			List<ILossFunction<Y>> lossFunctions) {
		super(scale);
		this.lossFunctions = lossFunctions;
	}
	
	public CompositeLossFunction(List<ILossFunction<Y>> lossFunctions) {
		this(1.0, lossFunctions);
	}
	
	@Override
	public String toString() {
		return new StringBuilder(CompositeLossFunction.class.getName())
				.append(" :: ").append(lossFunctions).toString();
	}
	
	@Override
	protected double doLossCalculation(IDataItem<Sentence> dataItem, Y label) {
		double loss = 0.0;
		for (final ILossFunction<Y> lossFunction : lossFunctions) {
			loss += lossFunction.calculateLoss(dataItem, label);
		}
		return loss;
	}
	
}
