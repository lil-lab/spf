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
package edu.cornell.cs.nlp.spf.parser.ccg.model;

import java.io.Serializable;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.hashvector.KeyArgs;

public interface IFeatureSet extends Serializable {

	/**
	 * The keys of the default features of a feature set are the features
	 * triggered by initial scorers. These features are not part of the model in
	 * the conventional way, but exist only as temporary placeholders.
	 * Therefore, the model doesn't allow updates that include them and need to
	 * keep track of them. This getter is used by {@link Model} for that
	 * purpose. The weight of these features is set to 1.0 and never changes.
	 */
	public Set<KeyArgs> getDefaultFeatures();

}
