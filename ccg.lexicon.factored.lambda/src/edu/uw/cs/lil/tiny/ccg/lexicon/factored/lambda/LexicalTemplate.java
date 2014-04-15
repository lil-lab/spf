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
package edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsTypeConsistent;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ReplaceExpression;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.counter.Counter;

public class LexicalTemplate implements Serializable {
	
	private static final long					serialVersionUID	= 7466276751228011529L;
	
	private final List<LogicalConstant>			arguments;
	
	private final String						origin;
	
	private final Category<LogicalExpression>	template;
	
	private final List<Type>					typeSignature;
	
	/**
	 * create a template by abstracting all of the constants. NOTE: we are
	 * assuming that every constant in the list appears somewhere in the logical
	 * expression for the category.
	 * 
	 * @param constants
	 * @param template
	 */
	private LexicalTemplate(List<LogicalConstant> constants,
			Category<LogicalExpression> template, String origin) {
		
		this.origin = origin;
		this.arguments = Collections.unmodifiableList(constants);
		this.template = template;
		final List<Type> types = new ArrayList<Type>(constants.size());
		for (final LogicalConstant constant : constants) {
			types.add(constant.getType());
		}
		this.typeSignature = Collections.unmodifiableList(types);
		
	}
	
	public static List<Pair<List<LogicalConstant>, LexicalTemplate>> doFactoring(
			final Category<LogicalExpression> inputCategory, boolean doMaximal,
			boolean doPartial, int maxConstantsInPartial, final String origin) {
		final Set<Pair<AbstractConstants.Placeholders, ? extends LogicalExpression>> factoring = AbstractConstants
				.of(inputCategory.getSem(), doMaximal, doPartial,
						maxConstantsInPartial);
		return ListUtils
				.map(factoring,
						new ListUtils.Mapper<Pair<AbstractConstants.Placeholders, ? extends LogicalExpression>, Pair<List<LogicalConstant>, LexicalTemplate>>() {
							
							@Override
							public Pair<List<LogicalConstant>, LexicalTemplate> process(
									Pair<AbstractConstants.Placeholders, ? extends LogicalExpression> obj) {
								return Pair.of(
										Collections.unmodifiableList(obj
												.first().originals),
										new LexicalTemplate(
												Collections.unmodifiableList(obj
														.first().placeholders),
												inputCategory
														.cloneWithNewSemantics(obj
																.second()),
												origin));
							}
						});
	}
	
	public static Pair<List<LogicalConstant>, LexicalTemplate> doFactoring(
			Category<LogicalExpression> inputCategory, String origin) {
		return doFactoring(inputCategory, true, false, 0, origin).get(0);
	}
	
	/**
	 * Given a string, read a lexical template from it.
	 */
	public static LexicalTemplate read(String line,
			ICategoryServices<LogicalExpression> categoryServices, String origin) {
		final int index = line.indexOf("-->");
		final String constantsString = line.substring(1, index - 1);
		final List<LogicalConstant> constants = new LinkedList<LogicalConstant>();
		if (!constantsString.equals("")) {
			for (final String constant : constantsString.split(", ")) {
				constants.add(LogicalConstant.read(constant));
			}
		}
		
		final String categoryString = line.substring(index + 3, line.length());
		
		return new LexicalTemplate(constants,
				categoryServices.parse(categoryString), origin);
	}
	
	public LexicalTemplate cloneWithNewSyntax(Syntax syntax) {
		return new LexicalTemplate(arguments, Category.create(syntax,
				template.getSem()), origin);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LexicalTemplate)) {
			return false;
		}
		final LexicalTemplate other = (LexicalTemplate) obj;
		if (arguments == null) {
			if (other.arguments != null) {
				return false;
			}
		} else if (!arguments.equals(other.arguments)) {
			return false;
		}
		if (template == null) {
			if (other.template != null) {
				return false;
			}
		} else if (!template.equals(other.template)) {
			return false;
		}
		return true;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public Category<LogicalExpression> getTemplateCategory() {
		return template;
	}
	
	public List<Type> getTypeSignature() {
		return typeSignature;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((arguments == null) ? 0 : arguments.hashCode());
		result = prime * result
				+ ((template == null) ? 0 : template.hashCode());
		return result;
	}
	
	public Category<LogicalExpression> makeCategory(Lexeme lexeme) {
		if (arguments.size() != lexeme.numConstants()) {
			return null;
		}
		LogicalExpression newSemantics = template.getSem();
		int i = 0;
		for (final LogicalConstant constant : lexeme.getConstants()) {
			final LogicalConstant variable = arguments.get(i);
			newSemantics = ReplaceExpression.of(newSemantics, variable,
					constant);
			i++;
		}
		if (!IsTypeConsistent.of(newSemantics)) {
			return null;
		}
		
		return template.cloneWithNewSemantics(newSemantics);
	}
	
	@Override
	public String toString() {
		return arguments + "-->" + template;
	}
	
	public static class AbstractConstants implements ILogicalExpressionVisitor {
		private final Map<Type, Counter>								counters	= new HashMap<Type, Counter>();
		private final boolean											doMaximal;
		private final boolean											doPartial;
		private final int												partialMaxConstants;
		private List<Pair<Placeholders, ? extends LogicalExpression>>	tempReturn	= null;
		
		private AbstractConstants(boolean doMaximal, boolean doPartial,
				int partialMaxConstants) {
			// Usage only through static 'of' method
			this.doMaximal = doMaximal;
			this.doPartial = doPartial;
			this.partialMaxConstants = partialMaxConstants;
		}
		
		public static Set<Pair<Placeholders, ? extends LogicalExpression>> of(
				LogicalExpression exp, boolean getMaximal, boolean getPartial,
				int partialMaxConstants) {
			final AbstractConstants visitor = new AbstractConstants(getMaximal,
					getPartial, partialMaxConstants);
			visitor.visit(exp);
			
			// Remove any empty factoring, unless it's a maximal one
			final Iterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = visitor.tempReturn
					.iterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (!pair.first().isMaximal() && pair.first().size() == 0) {
					iterator.remove();
				}
			}
			
			return new HashSet<Pair<Placeholders, ? extends LogicalExpression>>(
					visitor.tempReturn);
		}
		
		private static Pair<Placeholders, ? extends LogicalExpression> getAndRemoveMaximal(
				List<Pair<Placeholders, ? extends LogicalExpression>> pairs) {
			Pair<Placeholders, ? extends LogicalExpression> maximal = null;
			final Iterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = pairs
					.iterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (pair.first().isMaximal()) {
					if (maximal == null) {
						maximal = pair;
						iterator.remove();
					} else {
						throw new IllegalStateException(
								"found more than one maximal");
					}
				}
			}
			
			if (maximal == null) {
				throw new IllegalStateException(
						"expected a maximal pair, not found");
			}
			
			return maximal;
		}
		
		@Override
		public void visit(Lambda lambda) {
			// not visiting argument, since we are only abstracting constants.
			lambda.getBody().accept(this);
			final ListIterator<Pair<Placeholders, ? extends LogicalExpression>> iterator = tempReturn
					.listIterator();
			while (iterator.hasNext()) {
				final Pair<Placeholders, ? extends LogicalExpression> pair = iterator
						.next();
				if (pair.second() != null) {
					final LogicalExpression newBody = pair.second();
					if (newBody == lambda.getBody()) {
						iterator.set(Pair.of(pair.first(), lambda));
					} else {
						iterator.set(Pair.of(pair.first(),
								new Lambda(lambda.getArgument(), newBody)));
					}
					
				}
			}
		}
		
		@Override
		public void visit(Literal literal) {
			// Visit the predicate
			literal.getPredicate().accept(this);
			final List<Pair<Placeholders, ? extends LogicalExpression>> predicateReturn = tempReturn;
			
			final List<LogicalExpression> args = new ArrayList<LogicalExpression>(
					literal.getArguments());
			
			final List<List<Pair<Placeholders, ? extends LogicalExpression>>> argReturns = new ArrayList<List<Pair<Placeholders, ? extends LogicalExpression>>>(
					args.size());
			
			// In case of an order insensitive, sort the arguments by hashcode,
			// so the abstraction of constants will be insensitive to order,
			// when that order doesn't matter. TODO [yoav] [limitations] this
			// solution is still not perfect and might cause duplicate
			// templates/lexemes where such shouldn't exist. To fix it, we need
			// to change lexemes to hold a set of constants and not a list
			// (this, in turn, will cause difficulties in init templates)
			if (!literal.getPredicateType().isOrderSensitive()) {
				Collections.sort(args, new Comparator<LogicalExpression>() {
					public int compare(LogicalExpression l1,
							LogicalExpression l2) {
						return l1.hashCode() - l2.hashCode();
					}
				});
			}
			
			for (final LogicalExpression arg : args) {
				arg.accept(this);
				argReturns.add(tempReturn);
			}
			
			tempReturn = new LinkedList<Pair<Placeholders, ? extends LogicalExpression>>();
			
			if (doMaximal) {
				// Do the maximal combination by getting all the maximals.
				// Each returned list should have a single maximal, no more, no
				// less. The maximal is also removed to make it simpler to do
				// the partial ones later on.
				final Pair<Placeholders, ? extends LogicalExpression> predPair = getAndRemoveMaximal(predicateReturn);
				final List<Pair<Placeholders, ? extends LogicalExpression>> argPairs = ListUtils
						.map(argReturns,
								new ListUtils.Mapper<List<Pair<Placeholders, ? extends LogicalExpression>>, Pair<Placeholders, ? extends LogicalExpression>>() {
									@Override
									public Pair<Placeholders, ? extends LogicalExpression> process(
											List<Pair<Placeholders, ? extends LogicalExpression>> obj) {
										return getAndRemoveMaximal(obj);
									}
								});
				final Placeholders placeholder = predPair.first();
				int i = 0;
				boolean argsChanged = false;
				final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
						args.size());
				for (final Pair<Placeholders, ? extends LogicalExpression> argPair : argPairs) {
					placeholder.concat(argPair.first());
					newArgs.add(argPair.second());
					if (args.get(i) != argPair.second()) {
						argsChanged = true;
					}
					++i;
				}
				if (argsChanged || predPair.second() != literal.getPredicate()) {
					tempReturn.add(Pair.of(
							placeholder,
							new Literal(predPair.second() == literal
									.getPredicate() ? literal.getPredicate()
									: predPair.second(), newArgs)));
				} else {
					tempReturn.add(Pair.of(placeholder, literal));
				}
				
			}
			
			if (doPartial) {
				// At this point, if maximal pairs were present, they were
				// removed
				for (final Pair<Placeholders, ? extends LogicalExpression> predPair : predicateReturn) {
					for (final List<Pair<Placeholders, ? extends LogicalExpression>> argPairs : CollectionUtils
							.cartesianProduct(argReturns)) {
						final Placeholders placeholder = new Placeholders();
						placeholder.concat(predPair.first());
						int i = 0;
						boolean argsChanged = false;
						final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
								args.size());
						boolean fail = false;
						for (final Pair<Placeholders, ? extends LogicalExpression> argPair : argPairs) {
							if (placeholder.size() + argPair.first().size() <= partialMaxConstants) {
								placeholder.concat(argPair.first());
								newArgs.add(argPair.second());
								if (args.get(i) != argPair.second()) {
									argsChanged = true;
								}
								++i;
							} else {
								fail = true;
								break;
							}
						}
						if (!fail) {
							if (argsChanged
									|| predPair.second() != literal
											.getPredicate()) {
								tempReturn
										.add(Pair.of(
												placeholder,
												new Literal(
														predPair.second() == literal
																.getPredicate() ? literal
																.getPredicate()
																: predPair
																		.second(),
														newArgs)));
							} else {
								tempReturn.add(Pair.of(placeholder, literal));
							}
						}
					}
				}
			}
		}
		
		@Override
		public void visit(LogicalConstant logicalConstant) {
			if (FactoredLexiconServices.isFactorable(logicalConstant)) {
				tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
						3);
				
				if (doPartial) {
					// No factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> noFactoringPair = Pair
							.of(new Placeholders(), logicalConstant);
					tempReturn.add(noFactoringPair);
					// Partial factoring placeholder
					final Placeholders factoringPlaceholder = new Placeholders();
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(factoringPlaceholder,
									factoringPlaceholder.add(logicalConstant));
					tempReturn.add(factoringPair);
				}
				
				if (doMaximal) {
					// Maximal factoring placeholder
					final Placeholders factoringPlaceholder = new Placeholders(
							true);
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(factoringPlaceholder,
									factoringPlaceholder.add(logicalConstant));
					tempReturn.add(factoringPair);
				}
				final Type genType = LogicLanguageServices.getTypeRepository()
						.generalizeType(logicalConstant.getType());
				if (counters.containsKey(genType)) {
					counters.get(genType).inc();
				} else {
					counters.put(genType, new Counter(1));
				}
			} else {
				// No factoring, only empty placeholders
				
				tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
						2);
				
				if (doPartial) {
					// No factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> noFactoringPair = Pair
							.of(new Placeholders(), logicalConstant);
					tempReturn.add(noFactoringPair);
				}
				
				if (doMaximal) {
					// Maximal factoring (empty) placeholder
					final Pair<Placeholders, ? extends LogicalExpression> factoringPair = Pair
							.of(new Placeholders(true), logicalConstant);
					tempReturn.add(factoringPair);
				}
				
			}
		}
		
		@Override
		public void visit(LogicalExpression logicalExpression) {
			logicalExpression.accept(this);
		}
		
		@Override
		public void visit(Variable variable) {
			tempReturn = new ArrayList<Pair<Placeholders, ? extends LogicalExpression>>(
					2);
			
			// No factoring (empty) placeholder: maximal
			if (doMaximal) {
				final Pair<Placeholders, ? extends LogicalExpression> p = Pair
						.of(new Placeholders(true), variable);
				tempReturn.add(p);
			}
			
			// No factoring (empty) placeholder: partial
			if (doMaximal) {
				final Pair<Placeholders, ? extends LogicalExpression> p = Pair
						.of(new Placeholders(), variable);
				tempReturn.add(p);
			}
			
		}
		
		public class Placeholders {
			private final boolean				maximal;
			private final List<LogicalConstant>	originals		= new LinkedList<LogicalConstant>();
			private final List<LogicalConstant>	placeholders	= new LinkedList<LogicalConstant>();
			
			public Placeholders() {
				this(false);
			}
			
			public Placeholders(boolean maximal) {
				this.maximal = maximal;
			}
			
			public LogicalConstant add(LogicalConstant original) {
				originals.add(original);
				final LogicalConstant placeholder = makeConstant(original
						.getType());
				placeholders.add(placeholder);
				return placeholder;
			}
			
			public void concat(Placeholders other) {
				this.originals.addAll(other.originals);
				this.placeholders.addAll(other.placeholders);
			}
			
			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null) {
					return false;
				}
				if (getClass() != obj.getClass()) {
					return false;
				}
				final Placeholders other = (Placeholders) obj;
				if (!getOuterType().equals(other.getOuterType())) {
					return false;
				}
				if (originals == null) {
					if (other.originals != null) {
						return false;
					}
				} else if (!originals.equals(other.originals)) {
					return false;
				}
				if (placeholders == null) {
					if (other.placeholders != null) {
						return false;
					}
				} else if (!placeholders.equals(other.placeholders)) {
					return false;
				}
				return true;
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result
						+ ((originals == null) ? 0 : originals.hashCode());
				result = prime
						* result
						+ ((placeholders == null) ? 0 : placeholders.hashCode());
				return result;
			}
			
			public boolean isMaximal() {
				return maximal;
			}
			
			public int size() {
				return originals.size();
			}
			
			@Override
			public String toString() {
				return originals + (maximal ? " M-> " : " -> ") + placeholders;
			}
			
			private AbstractConstants getOuterType() {
				return AbstractConstants.this;
			}
			
			private LogicalConstant makeConstant(Type type) {
				final Type generalType = LogicLanguageServices
						.getTypeRepository().generalizeType(type);
				
				return LogicalConstant.createDynamic(LogicalConstant.makeName(
						"#"
								+ (counters.containsKey(generalType) ? counters
										.get(generalType).value() : 0)
								+ generalType, generalType), generalType);
			}
		}
		
	}
	
}
