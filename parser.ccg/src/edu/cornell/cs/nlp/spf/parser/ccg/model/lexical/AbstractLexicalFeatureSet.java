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
package edu.cornell.cs.nlp.spf.parser.ccg.model.lexical;

import java.util.Set;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.SimpleSyntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.parser.ccg.ILexicalParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.IParseStep;
import edu.cornell.cs.nlp.utils.collections.SetUtils;

public abstract class AbstractLexicalFeatureSet<DI extends IDataItem<?>, MR>
		implements IIndependentLexicalFeatureSet<DI, MR> {

	private static final long					serialVersionUID	= 6525970703793249000L;

	private final boolean						computeSyntaxAttributeFeatures;

	/**
	 * The primary tag for this feature set. This is the first key of the
	 * feature.
	 */
	protected final String						featureTag;

	/**
	 * Filter used to ignore certain lexical entries. When ignoring a lexical
	 * entry, no features are computed for it and its score is 0.0.
	 */
	protected final Predicate<LexicalEntry<MR>>	ignoreFilter;

	public AbstractLexicalFeatureSet(Predicate<LexicalEntry<MR>> ignoreFilter,
			boolean computeSyntaxAttributeFeatures, String featureTag) {
		this.ignoreFilter = ignoreFilter;
		this.computeSyntaxAttributeFeatures = computeSyntaxAttributeFeatures;
		this.featureTag = featureTag;
	}

	@Override
	public final boolean addEntry(LexicalEntry<MR> entry,
			IHashVector parametersVector) {
		if (ignoreFilter.test(entry)) {
			return doAddEntry(entry, parametersVector);
		}
		return false;
	}

	@Override
	public void setFeatures(IParseStep<MR> parseStep, IHashVector features,
			DI dataItem) {
		if (parseStep instanceof ILexicalParseStep && ignoreFilter
				.test(((ILexicalParseStep<MR>) parseStep).getLexicalEntry())) {
			setFeatures(((ILexicalParseStep<MR>) parseStep).getLexicalEntry(),
					features);
		}
	}

	@Override
	public final void setFeatures(LexicalEntry<MR> entry,
			IHashVector features) {
		if (entry == null || entry.isDynamic() || !ignoreFilter.test(entry)) {
			return;
		}

		doSetFeatures(entry, features);
	}

	protected abstract boolean doAddEntry(LexicalEntry<MR> entry,
			IHashVector parametersVector);

	protected void doSetFeatures(LexicalEntry<MR> entry, IHashVector features) {
		if (computeSyntaxAttributeFeatures) {
			if (entry.getCategory().getSyntax() instanceof ComplexSyntax) {
				// If the syntax is complex, discriminate between various
				// patterns of attribute usage. This will allow the system to
				// prefer one over the other, mainly in newly considered
				// templates during lexical induction. It also allows better
				// flow of information between lexical entries.

				final ComplexSyntax syntax = (ComplexSyntax) entry.getCategory()
						.getSyntax();

				final Syntax arg = syntax.getRight();
				final Syntax ret = syntax.getLeft();

				final Set<String> argAttributes = arg.getAttributes();
				final Set<String> retAttributes = ret.getAttributes();
				final boolean argHasVariable = arg.hasAttributeVariable();
				final boolean retHasVariable = ret.hasAttributeVariable();

				// Process attributes.
				if (!argAttributes.isEmpty() || !retAttributes.isEmpty()) {
					for (final String attribute : SetUtils.union(argAttributes,
							retAttributes)) {
						final boolean argContains = argAttributes
								.contains(attribute);
						final boolean retContains = retAttributes
								.contains(attribute);
						if (argContains && !retContains) {
							// Attribute appears in the argument, but not in the
							// return syntax.
							features.add(featureTag, "syntaxattrib", "argonly",
									arg instanceof SimpleSyntax
											? ((SimpleSyntax) arg).getLabel()
											: "complex",
									1.0);
						} else if (!argContains && retContains) {
							// Attribute appears in the return syntax, but not
							// in the argument.
							features.add(featureTag, "syntaxattrib", "retonly",
									ret instanceof SimpleSyntax
											? ((SimpleSyntax) ret).getLabel()
											: "complex",
									1.0);
						} else {
							// Attribute appears on both side of the slash (the
							// state of it not appearing on either side is not
							// possible).
							features.add(featureTag, "syntaxattrib",
									"agreement", 1.0);
						}
					}
				}

				// Process variables.
				if (argHasVariable || retHasVariable) {
					if (argHasVariable && !retHasVariable) {
						// Attribute appears in the argument, but not in the
						// return syntax.
						features.add(featureTag, "syntaxattrib", "varargonly",
								arg instanceof SimpleSyntax
										? ((SimpleSyntax) arg).getLabel()
										: "complex",
								1.0);
					} else if (!argHasVariable && retHasVariable) {
						// Attribute appears in the return syntax, but not
						// in the argument.
						features.add(featureTag, "syntaxattrib", "varretonly",
								ret instanceof SimpleSyntax
										? ((SimpleSyntax) ret).getLabel()
										: "complex",
								1.0);
					} else {
						// Attribute appears on both side of the slash (the
						// state of it not appearing on either side is not
						// possible).
						features.add(featureTag, "varsyntaxattrib", "agreement",
								1.0);
					}
				}

			}
		}
	}
}
