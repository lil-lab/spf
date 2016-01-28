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
package edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.utils.collections.IScorer;

public abstract class AbstractScaledScorerCreator<E, T extends IScorer<E>>
		implements IResourceObjectCreator<IScorer<E>> {
	
	@Override
	public final IScorer<E> create(Parameters parameters,
			IResourceRepository resourceRepo) {
		if (parameters.contains("scale")) {
			return new ScalingScorer<E>(
					Double.valueOf(parameters.get("scale")), createScorer(
							parameters, resourceRepo));
		} else {
			return createScorer(parameters, resourceRepo);
		}
	}
	
	protected abstract T createScorer(Parameters parameters,
			IResourceRepository resourceRepo);
	
}
