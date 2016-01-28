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
package edu.cornell.cs.nlp.spf.mr.lambda.exec.naive;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllFreeVariables;
import edu.cornell.cs.nlp.utils.composites.Pair;

public abstract class AbstractEvaluationServices<S> implements
		IEvaluationServices {
	
	private final Map<Pair<LogicalExpression, S>, CacheObject>	cache	= new HashMap<Pair<LogicalExpression, S>, CacheObject>();
	
	@Override
	public void cacheResult(LogicalExpression exp, Object result) {
		cache.put(Pair.of(exp, currentState()), new CacheObject(
				GetAllFreeVariables.of(exp), result));
	}
	
	@Override
	public void denotationChanged(Variable variable) {
		final Iterator<Entry<Pair<LogicalExpression, S>, CacheObject>> iterator = cache
				.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().freeVariables.contains(variable)) {
				iterator.remove();
			}
		}
	}
	
	@Override
	public Object evaluateConstant(LogicalConstant logicalConstant) {
		
		// Try to treat the constant as a number
		final Long num = LogicLanguageServices
				.logicalExpressionToInteger(logicalConstant);
		if (num != null) {
			return new Double(num);
		}
		
		// Case true:t
		if (LogicLanguageServices.getTrue().equals(logicalConstant)) {
			return true;
		}
		
		// Case false:t
		if (LogicLanguageServices.getFalse().equals(logicalConstant)) {
			return false;
		}
		
		// Unknown constant
		return null;
	}
	
	@Override
	public Object getFromCache(LogicalExpression exp) {
		return cache.get(Pair.of(exp, currentState())).cachedObject;
	}
	
	@Override
	public boolean isCached(LogicalExpression exp) {
		return cache.containsKey(Pair.of(exp, currentState()));
	}
	
	@Override
	public boolean isInterpretable(LogicalConstant constant) {
		return LogicLanguageServices.isCoordinationPredicate(constant);
	}
	
	protected void clearStateFromCache(S state) {
		final Iterator<Entry<Pair<LogicalExpression, S>, CacheObject>> iterator = cache
				.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getKey().second().equals(state)) {
				iterator.remove();
			}
		}
	}
	
	protected abstract S currentState();
	
	private class CacheObject {
		private final Object		cachedObject;
		private final Set<Variable>	freeVariables;
		
		public CacheObject(Set<Variable> freeVariables, Object cachedObject) {
			this.freeVariables = freeVariables;
			this.cachedObject = cachedObject;
		}
		
	}
	
}
