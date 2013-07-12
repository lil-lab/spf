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
import edu.uw.cs.lil.tiny.data.utils.IValidator;

/**
 * Represents a conjunction between a list of validators.
 * 
 * @author Yoav Artzi
 * @param <Y>
 */
public class CompositeValidator<Y> implements
		IValidator<IDataItem<Sentence>, Y> {
	
	private final List<IValidator<IDataItem<Sentence>, Y>>	validators;
	
	public CompositeValidator(
			List<IValidator<IDataItem<Sentence>, Y>> validators) {
		this.validators = validators;
	}
	
	@Override
	public boolean isValid(IDataItem<Sentence> dataItem, Y label) {
		for (final IValidator<IDataItem<Sentence>, Y> validator : validators) {
			if (!validator.isValid(dataItem, label)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(CompositeValidator.class.getName()).append(
				validators).toString();
	}
}
