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
package edu.uw.cs.lil.tiny.genlex.ccg.unification.split;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.PowerSet;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetAllFreeVariables;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ReplaceExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Do higher-order unification splits. X/Z : \x.h(x) ==> X/Y : f Y/Z : g s.t.
 * \x.f(g(x)) = \x.h(x) (and the backwards case also)
 * 
 * @author Luke Zettlemoyer
 */
public class MakeCompositionSplits implements ILogicalExpressionVisitor {
	
	public static final ILogger							LOG		= LoggerFactory
																		.create(MakeCompositionSplits.class
																				.getName());
	
	private final ICategoryServices<LogicalExpression>	categoryServices;
	private final ComplexCategory<LogicalExpression>	originalCategory;
	private final Lambda								originalLambda;
	
	private final Set<SplittingPair>					splits	= new HashSet<SplittingPair>();
	
	/**
	 * Usage only through static 'of' method.
	 * 
	 * @param originalCategory
	 * @param originalLambda
	 * @param categoryServices
	 */
	private MakeCompositionSplits(
			ComplexCategory<LogicalExpression> originalCategory,
			Lambda originalLambda,
			ICategoryServices<LogicalExpression> categoryServices) {
		this.originalCategory = originalCategory;
		this.originalLambda = originalLambda;
		this.categoryServices = categoryServices;
	}
	
	/**
	 * Create the empty split for the given category.
	 * 
	 * @param originalCategory
	 *            Assumed to be a complex category with a complex typed logical
	 *            expression.
	 * @param categoryServices
	 * @return
	 */
	private static SplittingPair doEmptySplit(
			ComplexCategory<LogicalExpression> originalCategory,
			ICategoryServices<LogicalExpression> categoryServices) {
		// Create the unity function logical expression (lambda x x)
		final Variable unityVariable = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(
						originalCategory.getSem().getType().getDomain()));
		final LogicalExpression unityFunction = new Lambda(unityVariable,
				unityVariable);
		
		// Create the X/X category with the unity function as its semantics
		final Syntax originalCategoryDomain = originalCategory.getSyntax()
				.getRight();
		final Slash originalSlash = originalCategory.getSlash();
		final ComplexCategory<LogicalExpression> newCategory = new ComplexCategory<LogicalExpression>(
				new ComplexSyntax(originalCategoryDomain,
						originalCategoryDomain, originalSlash), unityFunction);
		
		// Create the split. Don't allow crossing composition.
		final SplittingPair split;
		if (originalSlash == Slash.BACKWARD) {
			// Cator goes on right
			split = new SplittingPair(newCategory, originalCategory);
		} else {
			// Cator goes on left
			split = new SplittingPair(originalCategory, newCategory);
		}
		
		// Test the split
		final Category<LogicalExpression> newRoot = categoryServices.compose(
				originalCategory, newCategory, 0);
		if (!originalCategory.equals(newRoot)) {
			LOG.error("ERROR: error in Cat composition split");
			LOG.error("%s --> %s != $s", split, newRoot, originalCategory);
		}
		
		return split;
	}
	
	static Set<SplittingPair> of(Category<LogicalExpression> originalCategory,
			ICategoryServices<LogicalExpression> categoryServices) {
		
		// Check if can split
		if (!(originalCategory instanceof ComplexCategory)
				|| !(originalCategory.getSem() instanceof Lambda)) {
			// Case not a function, can't split
			return Collections.emptySet();
		}
		
		final ComplexCategory<LogicalExpression> complexCategory = (ComplexCategory<LogicalExpression>) originalCategory;
		
		if (complexCategory.getSlash() == Slash.VERTICAL) {
			// Case the category has a vertical slash, so can't split it
			return Collections.emptySet();
		}
		
		// Create the visitor and visit the logical expression
		final MakeCompositionSplits visitor = new MakeCompositionSplits(
				complexCategory, (Lambda) complexCategory.getSem(),
				categoryServices);
		visitor.visit(originalCategory.getSem());
		
		// Empty split
		visitor.splits.add(doEmptySplit(complexCategory, categoryServices));
		
		return visitor.splits;
	}
	
	@Override
	public void visit(Lambda lambda) {
		lambda.getArgument().accept(this);
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		if (literal.getPredicateType() instanceof RecursiveComplexType) {
			// Case recursive predicate, we to extract subsets of its arguments
			splits.addAll(doSplitsForRecursivePredicate(literal));
		}
		
		// Collect all the free variables. We need one of them to match the
		// outermost variables of the original Lambda expression. The total
		// number has to be less than the threshold. Also, skip literals where
		// the predicate is a variable.
		final Set<Variable> freeVars = GetAllFreeVariables.of(literal);
		if (freeVars.size() <= SplittingServices.MAX_NUM_VARS
				&& freeVars.remove(originalLambda.getArgument())
				&& !(literal.getPredicate() instanceof Variable)) {
			for (final List<Variable> order : SplittingServices
					.allOrders(freeVars)) {
				final SplittingPair split = doSplit(literal, order);
				if (split != null) {
					splits.add(split);
				}
			}
		}
		
		// NOTE: we do not call literal.getPredicate().accept(this) because we
		// don't want to pull out predicate names
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
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
	
	private SplittingPair createSplittingPair(Lambda f, LogicalExpression g) {
		// Simplify f and g
		final LogicalExpression simplifiedF = Simplify.of(f);
		final LogicalExpression simlifiedG = Simplify.of(g);
		
		// Create the categories for the composition
		final Slash slash = originalCategory.getSlash();
		final Category<LogicalExpression> sharedCategory = Category
				.create(SplittingServices.typeToSyntax(simplifiedF.getType()
						.getDomain()));
		final ComplexCategory<LogicalExpression> fCategory = new ComplexCategory<LogicalExpression>(
				new ComplexSyntax(originalCategory.getSyntax().getLeft(),
						sharedCategory.getSyntax(), slash), simplifiedF);
		final ComplexCategory<LogicalExpression> gCategory = new ComplexCategory<LogicalExpression>(
				new ComplexSyntax(sharedCategory.getSyntax(), originalCategory
						.getSyntax().getRight(), slash), simlifiedG);
		
		// Create the splitting pair. Don't allow crossing composition
		final SplittingPair newSplit;
		if (slash == Slash.BACKWARD) {
			// Cator goes on right
			newSplit = new SplittingPair(gCategory, fCategory);
		} else {
			// Cator goes on left
			newSplit = new SplittingPair(fCategory, gCategory);
		}
		
		// Error checking
		final Category<LogicalExpression> composed = categoryServices.compose(
				fCategory, gCategory, 0);
		if (!originalCategory.equals(composed)) {
			LOG.error("ERROR: bad Cat composition split");
			LOG.error("%s ---> %s != %s", newSplit, composed, originalCategory);
		}
		
		return newSplit;
	}
	
	/**
	 * Extract the entire subExpression
	 * 
	 * @param subExpression
	 * @param argumentOrder
	 *            The free variables in subExpression except the first variables
	 *            in originalLambda, which is assumed to be in subExpression as
	 *            well.
	 * @return
	 */
	private SplittingPair doSplit(LogicalExpression subExpression,
			List<Variable> argumentOrder) {
		
		// The h expression
		final Variable rootArg = originalLambda.getArgument();
		
		// Create variables for the objects we will pull out
		final List<Variable> newVars = new LinkedList<Variable>();
		// The variable x, such that h = \lambda x f(g(x))
		newVars.add(rootArg);
		// The rest of the arguments to the function g
		newVars.addAll(argumentOrder);
		
		// Create g, such that h = \lambda x f(g(x))
		final LogicalExpression g = SplittingServices.makeExpression(newVars,
				subExpression);
		
		// Create f, such that h = \lambda x f(g(x))
		newVars.remove(rootArg);
		final Variable compositionArgument = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(g.getType().getRange()));
		final LogicalExpression embeddedApplication = SplittingServices
				.makeAssignment(newVars, compositionArgument);
		final LogicalExpression newBody = ReplaceExpression.of(
				originalLambda.getBody(), subExpression, embeddedApplication);
		final Lambda f = new Lambda(compositionArgument, newBody);
		
		// Verify that f has no free arguments. Can happen if rootArg appears in
		// another part of the expression.
		if (GetAllFreeVariables.of(f).size() == 0) {
			// Create the splitting pair and return it
			return createSplittingPair(f, g);
		} else {
			return null;
		}
	}
	
	/**
	 * Create splits for a literal with a order-insensitive recursive predicate.
	 * 
	 * @param literal
	 * @return
	 */
	private Set<SplittingPair> doSplitsForRecursiveOrderInsensitivePredicate(
			Literal literal) {
		final RecursiveComplexType predicateType = (RecursiveComplexType) literal
				.getPredicateType();
		final Set<SplittingPair> newSplits = new HashSet<SplittingServices.SplittingPair>();
		final List<LogicalExpression> args = new LinkedList<LogicalExpression>(
				literal.getArguments());
		
		// Variable for x in \lambda x . f(g(x))
		final Variable rootArg = originalLambda.getArgument();
		
		// Iterate over all subsets of arguments
		for (final List<LogicalExpression> argsSubset : new PowerSet<LogicalExpression>(
				args)) {
			final int size = argsSubset.size();
			if (size >= predicateType.getMinArgs()
					&& size < SplittingServices.MAX_NUM_SUBS
					&& size < args.size()) {
				// Case the subset of arguments is within the size limits. Not
				// too small (the single argument is dealt with separately) and
				// not too big (need to leave something behind).
				
				// Body of g
				final Literal gBody = new Literal(literal.getPredicate(),
						new ArrayList<LogicalExpression>(argsSubset));
				final Set<Variable> gFreeVars = GetAllFreeVariables.of(gBody);
				if (gFreeVars.size() <= SplittingServices.MAX_NUM_VARS
						&& gFreeVars.remove(rootArg)) {
					// Case the number of free variables is under the threshold
					// and the set of free variables contains the root argument,
					// otherwise skip this subset. Also remove the root argument
					// from the free variables, so we an glue at the prefix
					// of the lambda expression we will create.
					
					// Iterate over all possible variable orderings
					for (final List<Variable> newArgOrder : SplittingServices
							.allOrders(gFreeVars)) {
						
						// Construct the variables list
						final List<Variable> newVars = new LinkedList<Variable>();
						// Put the root argument at the beginning of the
						// variables list (this is the variable x, such that h =
						// \lambda x f(g(x))).
						newVars.add(rootArg);
						// And the rest of the variables
						newVars.addAll(newArgOrder);
						
						// Create the new function g, to extract
						final LogicalExpression g = SplittingServices
								.makeExpression(newVars, gBody);
						
						// Create f, such that h = \lambda x f(g(x))
						newVars.remove(rootArg);
						final Variable compositionArg = new Variable(
								LogicLanguageServices.getTypeRepository()
										.generalizeType(g.getType().getRange()));
						final LogicalExpression embeddedApplication = SplittingServices
								.makeAssignment(newVars, compositionArg);
						final List<LogicalExpression> newLiteralArguments = new ArrayList<LogicalExpression>();
						newLiteralArguments.addAll(literal.getArguments());
						// Remove the subset of arguments we extracted
						for (final LogicalExpression gone : argsSubset) {
							newLiteralArguments.remove(gone);
						}
						// Add the embedded application instead
						newLiteralArguments.add(embeddedApplication);
						final LogicalExpression newLiteral = new Literal(
								literal.getPredicate(), newLiteralArguments);
						// Replace the original literal with the new one. Only
						// support replaceing all occurrences.
						final LogicalExpression fBody = ReplaceExpression.of(
								originalLambda.getBody(), literal, newLiteral);
						
						final Lambda f = new Lambda(compositionArg, fBody);
						
						// Verify that f has no free variables (can happen if
						// rootArg appears in other places)
						if (GetAllFreeVariables.of(f).size() == 0) {
							newSplits.add(createSplittingPair(f, g));
						}
					}
				}
			}
		}
		return newSplits;
	}
	
	private Set<SplittingPair> doSplitsForRecursiveOrderSensitivePredicate(
			Literal literal) {
		final RecursiveComplexType predicateType = (RecursiveComplexType) literal
				.getPredicateType();
		final Set<SplittingPair> newSplits = new HashSet<SplittingServices.SplittingPair>();
		final List<LogicalExpression> args = new LinkedList<LogicalExpression>(
				literal.getArguments());
		
		// Variable for x in \lambda x . f(g(x))
		final Variable rootArg = originalLambda.getArgument();
		
		// Iterate over all span lengths
		for (int length = predicateType.getMinArgs(); length <= SplittingServices.MAX_NUM_SUBS; length++) {
			// Iterate over all spans of the given length
			for (int begin = 0; begin < args.size() - length; begin++) {
				// The current span of arguments
				final List<LogicalExpression> argsSublist = CollectionUtils
						.subList(args, begin, begin + length);
				
				// Body of g
				final Literal gBody = new Literal(literal.getPredicate(),
						new ArrayList<LogicalExpression>(argsSublist));
				final Set<Variable> gFreeVars = GetAllFreeVariables.of(gBody);
				if (gFreeVars.size() <= SplittingServices.MAX_NUM_VARS
						&& gFreeVars.remove(rootArg)) {
					// Case the number of free variables is under the threshold
					// and the set of free variables contains the root argument,
					// otherwise skip this subset. Also remove the root argument
					// from the free variables, so we an glue at the prefix
					// of the lambda expression we will create.
					
					// Iterate over all possible variable orderings
					for (final List<Variable> newArgOrder : SplittingServices
							.allOrders(gFreeVars)) {
						
						// Construct the variables list
						final List<Variable> newVars = new LinkedList<Variable>();
						// Put the root argument at the beginning of the
						// variables list (this is the variable x, such that h =
						// \lambda x f(g(x))).
						newVars.add(rootArg);
						// And the rest of the variables
						newVars.addAll(newArgOrder);
						
						// Create the new function g, to extract
						final LogicalExpression g = SplittingServices
								.makeExpression(newVars, gBody);
						
						// Create f, such that h = \lambda x f(g(x))
						newVars.remove(rootArg);
						final Variable compositionArg = new Variable(
								LogicLanguageServices.getTypeRepository()
										.generalizeType(g.getType().getRange()));
						final LogicalExpression embeddedApplication = SplittingServices
								.makeAssignment(newVars, compositionArg);
						final List<LogicalExpression> newLiteralArguments = new ArrayList<LogicalExpression>();
						newLiteralArguments.addAll(literal.getArguments());
						// Remove the sub-list of arguments we extracted
						for (int i = 0; i < length; i++) {
							newLiteralArguments.remove(begin);
						}
						// Add the embedded application instead
						newLiteralArguments.add(begin, embeddedApplication);
						final LogicalExpression newLiteral = new Literal(
								literal.getPredicate(), newLiteralArguments);
						// Replace the original literal with the new one. Only
						// support replacing all occurrences.
						final LogicalExpression fBody = ReplaceExpression.of(
								originalLambda.getBody(), literal, newLiteral);
						
						final Lambda f = new Lambda(compositionArg, fBody);
						
						// Verify that f has no free variables (can happen if
						// rootArg appears in other places)
						if (GetAllFreeVariables.of(f).size() == 0) {
							newSplits.add(createSplittingPair(f, g));
						}
					}
				}
			}
		}
		return newSplits;
	}
	
	private Set<SplittingPair> doSplitsForRecursivePredicate(Literal literal) {
		if (literal.getPredicateType() instanceof RecursiveComplexType) {
			if (literal.getPredicateType().isOrderSensitive()) {
				// Case order sensitive predicates, such as do_seq:<a+,a>
				return doSplitsForRecursiveOrderSensitivePredicate(literal);
			} else {
				// Case order insensitive predicates, such as and:<t*,t>
				return doSplitsForRecursiveOrderInsensitivePredicate(literal);
			}
		} else {
			return Collections.emptySet();
		}
	}
}
