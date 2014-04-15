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
package edu.uw.cs.lil.tiny.data.singlesentence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetConstCounts;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.utils.counter.Counter;

/**
 * Represents a single sentence and its logical form for supervised learning.
 * 
 * @author Yoav Artzi
 */
public class SingleSentence implements
		ILabeledDataItem<Sentence, LogicalExpression> {
	private static final long					serialVersionUID	= 5434665811874050978L;
	private final Map<LogicalConstant, Counter>	constCounts;
	
	private final Map<String, Counter>			predArgCounts;
	private final Map<String, String>			properties;
	
	private final LogicalExpression				semantics;
	private final Sentence						sentence;
	
	public SingleSentence(Sentence sentence, LogicalExpression semantics) {
		this(sentence, semantics, new HashMap<String, String>());
	}
	
	public SingleSentence(Sentence sentence, LogicalExpression semantics,
			Map<String, String> properties) {
		this.sentence = sentence;
		this.semantics = semantics;
		this.properties = Collections.unmodifiableMap(properties);
		
		// Prepare pruning data.
		constCounts = GetConstCounts.of(semantics);
		predArgCounts = GetConstHeadPairCounts.of(semantics);
		
		// Removed special predicates that we can't enforce strict count for:
		// constants that may be removed during simplification and the array
		// index predicate.
		final Iterator<Entry<LogicalConstant, Counter>> iterator = constCounts
				.entrySet().iterator();
		while (iterator.hasNext()) {
			final LogicalConstant pred = iterator.next().getKey();
			if (LogicLanguageServices.isCollpasibleConstant(pred)
					|| LogicLanguageServices.isArrayIndexPredicate(pred)) {
				iterator.remove();
			}
		}
		
	}
	
	private static boolean ignoreConstant(LogicalExpression pred) {
		return LogicLanguageServices.isCollpasibleConstant(pred)
				|| LogicLanguageServices.isArrayIndexPredicate(pred);
	}
	
	@Override
	public double calculateLoss(LogicalExpression label) {
		if (label.equals(semantics)) {
			return 0.0;
		} else {
			return 1.0;
		}
	}
	
	@Override
	public LogicalExpression getLabel() {
		return semantics;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	@Override
	public Sentence getSample() {
		return sentence;
	}
	
	@Override
	public boolean isCorrect(LogicalExpression label) {
		return label.equals(semantics);
	}
	
	@Override
	public boolean prune(LogicalExpression y) {
		// Single constant counts.
		final Map<LogicalConstant, Counter> currentConstCounts = GetConstCounts
				.of(y);
		for (final Map.Entry<LogicalConstant, Counter> entry : currentConstCounts
				.entrySet()) {
			// Ignore constants that we can't track properly.
			if (!ignoreConstant(entry.getKey())) {
				if (!constCounts.containsKey(entry.getKey())) {
					// Case the constant is not present in the label.
					return true;
				} else if (constCounts.get(entry.getKey()).value() < entry
						.getValue().value()) {
					// Case constant appears too many times.
					return true;
				}
			}
		}
		
		// Constant pairs counts.
		final Map<String, Counter> currentPredArgCounts = GetConstHeadPairCounts
				.of(y);
		for (final Map.Entry<String, Counter> entry : currentPredArgCounts
				.entrySet()) {
			if (!predArgCounts.containsKey(entry.getKey())) {
				// Prune for unexpected pairing.
				return true;
			} else if (predArgCounts.get(entry.getKey()).value() < entry
					.getValue().value()) {
				// Prune because of too many pairs.
				return true;
			}
		}
		return false;
	}
	
	@Override
	public double quality() {
		return 1.0;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(sentence.toString()).append('\n')
				.append(semantics).toString();
	}
	
	/**
	 * Create map of string ids to counts. Each string id is the predicate name
	 * paired with the position number and head string for each of its
	 * arguments.
	 * 
	 * @author Luke Zettlemoyer
	 */
	private static class GetConstHeadPairCounts implements
			ILogicalExpressionVisitor {
		private final Map<String, Counter>	constants	= new HashMap<String, Counter>();
		
		private GetConstHeadPairCounts() {
			// Usage only through static 'of' method.
		}
		
		public static Map<String, Counter> of(LogicalExpression exp) {
			final GetConstHeadPairCounts visitor = new GetConstHeadPairCounts();
			visitor.visit(exp);
			return visitor.getConstantHeadCounts();
		}
		
		public Map<String, Counter> getConstantHeadCounts() {
			return constants;
		}
		
		@Override
		public void visit(Lambda lambda) {
			lambda.getBody().accept(this);
		}
		
		@Override
		public void visit(Literal literal) {
			final LogicalExpression pred = literal.getPredicate();
			pred.accept(this);
			int i = 0;
			// Don't count for predicates that may disappear later by
			// simplifying the logical expression.
			final boolean counting = pred instanceof LogicalConstant
					&& !ignoreConstant(pred);
			for (final LogicalExpression arg : literal.getArguments()) {
				final LogicalConstant head = GetHeadConst.of(arg);
				// Skip arguments that might disappear later by simplifying the
				// logical expression, but do visit them to check their
				// sub-expressions.
				if (counting && head != null && !ignoreConstant(head)) {
					final String id = new StringBuilder(pred.toString())
							.append(i).append(head).toString();
					if (!constants.containsKey(id)) {
						// TODO [yoav] [ugly] Use something better than a
						// stringed ID
						// here.
						constants.put(id, new Counter());
					}
					constants.get(id).inc();
				}
				arg.accept(this);
				if (literal.getPredicateType().isOrderSensitive()) {
					// Increase the index for the next argument.
					++i;
				}
				
			}
		}
		
		@Override
		public void visit(LogicalConstant logicalConstant) {
			// Nothing to do
		}
		
		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}
		
		@Override
		public void visit(Variable variable) {
			// Nothing to do
		}
	}
	
	/**
	 * Returns the head constant of the expression.
	 * 
	 * @author Luke Zettlemoyer
	 */
	private static class GetHeadConst implements ILogicalExpressionVisitor {
		
		private LogicalConstant	headConst	= null;
		
		private GetHeadConst() {
			// Usage only through static 'of' method.
		}
		
		public static LogicalConstant of(LogicalExpression exp) {
			final GetHeadConst visitor = new GetHeadConst();
			visitor.visit(exp);
			return visitor.getHeadConst();
		}
		
		public LogicalConstant getHeadConst() {
			return headConst;
		}
		
		@Override
		public void visit(Lambda lambda) {
			lambda.getBody().accept(this);
		}
		
		@Override
		public void visit(Literal literal) {
			literal.getPredicate().accept(this);
		}
		
		@Override
		public void visit(LogicalConstant logicalConstant) {
			headConst = logicalConstant;
		}
		
		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}
		
		@Override
		public void visit(Variable variable) {
			// Nothing to do, don't want to return a variable.
		}
	}
}
