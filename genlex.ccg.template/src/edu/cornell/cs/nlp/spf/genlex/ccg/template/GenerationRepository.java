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
package edu.cornell.cs.nlp.spf.genlex.ccg.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Collections2;

import edu.cornell.cs.nlp.spf.base.collections.PowerSetWithFixedSize;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringSignature;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Stores a set of templates and syntactic attributes for lexical generation.
 *
 * @author Yoav Artzi
 */
public class GenerationRepository implements Serializable {

	public static final ILogger								LOG					= LoggerFactory
			.create(GenerationRepository.class);

	private static final long								serialVersionUID	= -3262020091296833093L;

	private final Map<Integer, List<List<String>>>			arrityAndAttributes;
	private final Set<String>								attributes;
	private final Set<FactoringSignature>					signatures;
	private final Set<LexicalTemplate>						templates;
	private final Map<LexicalTemplate, List<List<String>>>	templatesAndAttributes;

	public GenerationRepository() {
		this(new HashSet<>(), new HashSet<>());
	}

	public GenerationRepository(Set<LexicalTemplate> templates,
			Set<String> attributes) {
		this(new HashSet<LexicalTemplate>(),
				new HashMap<LexicalTemplate, List<List<String>>>(), attributes);
		for (final LexicalTemplate template : templates) {
			addTemplate(template);
		}
	}

	protected GenerationRepository(Set<LexicalTemplate> templates,
			Map<LexicalTemplate, List<List<String>>> templatesAndAttributes,
			Set<String> attributes) {
		this.templates = templates;
		this.templatesAndAttributes = templatesAndAttributes;
		this.attributes = attributes;
		this.signatures = templates.stream().map(t -> t.getSignature())
				.collect(Collectors.toCollection(() -> new HashSet<>()));
		this.arrityAndAttributes = signatures.stream()
				.collect(Collectors.toMap(
						signature -> signature.getNumAttributes(),
						signature -> createAttributePermutations(
								signature.getNumAttributes()),
						(o1, o2) -> o1, () -> new HashMap<>()));
	}

	public boolean addTemplate(LexicalTemplate template) {
		if (!templates.contains(template)) {
			LOG.debug("Trying to add template: %s", template);
			// First verify that we really need this template. There's an
			// existing issue with spurious ambiguity in factoring. This fix
			// is a brute-force hack to try to suppress it when using GENLEX.
			// When there are many templates, this process is going to explode
			final Iterator<LexicalTemplate> iterator = templates.iterator();
			while (iterator.hasNext()) {
				final LexicalTemplate existingTemplate = iterator.next();
				final int numAttributes = existingTemplate.getSignature()
						.getNumAttributes();
				if (numAttributes == template.getSignature().getNumAttributes()
						&& existingTemplate.getSignature().getTypes()
								.size() == template.getSignature().getTypes()
										.size()) {
					// Create dummy attributes list.
					final List<String> dummyAttributes = new ArrayList<String>(
							numAttributes);
					for (int i = 0; i < numAttributes; ++i) {
						dummyAttributes.add("dummy" + i);
					}
					for (final List<LogicalConstant> permutation : Collections2
							.permutations(template.getArguments())) {
						// Create a dummy lexeme.
						final Lexeme dummy = new Lexeme(TokenSeq.of(),
								permutation, dummyAttributes);
						if (existingTemplate.isValid(dummy)) {
							final Category<LogicalExpression> application = existingTemplate
									.apply(dummy);
							if (application != null && application.equals(
									template.apply(new Lexeme(TokenSeq.of(),
											template.getArguments(),
											dummyAttributes)))) {
								// Skip adding this template.
								LOG.debug(
										"Ignoring template (spurious ambiguity): %s",
										template);
								LOG.debug("... detected duplicate of: %s",
										existingTemplate);
								return false;
							}
						}
					}
				}
			}
			LOG.info(
					"Adding new template to generation repository (%d constants, %d attributes): %s",
					template.getArguments().size(),
					template.getSignature().getNumAttributes(), template);
			templates.add(template);
			if (signatures.add(template.getSignature()) && !arrityAndAttributes
					.containsKey(template.getSignature().getNumAttributes())) {
				arrityAndAttributes.put(
						template.getSignature().getNumAttributes(),
						createAttributePermutations(
								template.getSignature().getNumAttributes()));
			}

			templatesAndAttributes.put(template, Collections.unmodifiableList(
					createAllAttributesPermutations(template)));
			return true;
		}

		return false;
	}

	public List<List<String>> getAttributeLists(int numAttributes) {
		assert arrityAndAttributes.containsKey(
				numAttributes) : "Assumes the attribute list length is indexed";
		return arrityAndAttributes.get(numAttributes);
	}

	public List<List<String>> getAttributeLists(LexicalTemplate template) {
		return templatesAndAttributes.get(template);
	}

	public Set<FactoringSignature> getSignatures() {
		return Collections.unmodifiableSet(signatures);
	}

	public Set<LexicalTemplate> getTemplates() {
		return Collections.unmodifiableSet(templates);
	}

	public void init(IModelImmutable<?, LogicalExpression> model) {
		final Collection<LexicalEntry<LogicalExpression>> lexicalEntries = model
				.getLexicon().toCollection();
		final Set<LexicalTemplate> modelTemplates = new HashSet<LexicalTemplate>();
		final Set<String> modelAttributes = new HashSet<String>();
		for (final LexicalEntry<LogicalExpression> entry : lexicalEntries) {
			final FactoredLexicalEntry factored = FactoringServices
					.factor(entry);
			modelTemplates.add(factored.getTemplate());
			modelAttributes.addAll(factored.getLexeme().getAttributes());
		}
		init(modelTemplates, modelAttributes);
	}

	public void init(Set<LexicalTemplate> initTemplates,
			Set<String> initAttributes) {
		templates.clear();
		attributes.clear();
		signatures.clear();
		templatesAndAttributes.clear();
		arrityAndAttributes.clear();
		attributes.addAll(initAttributes);
		for (final LexicalTemplate template : initTemplates) {
			addTemplate(template);
		}
	}

	public int numSignatures() {
		return signatures.size();
	}

	public int numTemplates() {
		return templates.size();
	}

	public GenerationRepositoryWithConstants setConstants(
			Set<LogicalConstant> constants) {
		final long startTime = System.currentTimeMillis();
		long aggregate = 0;
		final Map<FactoringSignature, List<List<LogicalConstant>>> signaturesAndSeqs = new HashMap<FactoringSignature, List<List<LogicalConstant>>>();
		for (final FactoringSignature signature : signatures) {
			final List<List<LogicalConstant>> seqs = Collections
					.unmodifiableList(
							createPotentialConstantSeqs(constants, signature));
			aggregate += seqs.size();
			signaturesAndSeqs.put(signature, seqs);
		}
		LOG.debug(
				"Initialized generation repository with %d constants sequences (%.3fsec)",
				aggregate, (System.currentTimeMillis() - startTime) / 1000.0);
		return new GenerationRepositoryWithConstants(constants, templates,
				templatesAndAttributes, attributes, signaturesAndSeqs);
	}

	/**
	 * Create all attribute permutations that can be used (within a lexeme) to
	 * initialize this template.
	 */
	private List<List<String>> createAllAttributesPermutations(
			LexicalTemplate template) {
		if (template.getSignature().getNumAttributes() == 0) {
			return ListUtils
					.createSingletonList(Collections.<String> emptyList());
		}

		final List<List<String>> attributePermutations = new LinkedList<List<String>>();
		for (final List<String> subset : new PowerSetWithFixedSize<String>(
				attributes, template.getSignature().getNumAttributes())) {
			// Create all permutations of this subset of attributes.
			for (final List<String> permutation : Collections2
					.permutations(subset)) {
				if (template.apply(new Lexeme(TokenSeq.of(),
						template.getArguments(), permutation)) != null) {
					attributePermutations
							.add(Collections.unmodifiableList(permutation));
				}
			}
		}
		return attributePermutations;
	}

	private List<List<String>> createAttributePermutations(int length) {
		if (length == 0) {
			return ListUtils
					.createSingletonList(Collections.<String> emptyList());
		}

		final List<List<String>> attributePermutations = new LinkedList<List<String>>();
		for (final List<String> subset : new PowerSetWithFixedSize<String>(
				attributes, length)) {
			// Create all permutations of this subset of attributes.
			for (final List<String> permutation : Collections2
					.permutations(subset)) {
				attributePermutations
						.add(Collections.unmodifiableList(permutation));
			}
		}
		return attributePermutations;
	}

	protected List<List<LogicalConstant>> createPotentialConstantSeqs(
			Set<LogicalConstant> constants, FactoringSignature signature) {
		if (signature.getTypes().isEmpty()) {
			// Case no arguments, create the empty list.
			return ListUtils.createSingletonList(
					Collections.<LogicalConstant> emptyList());
		} else {
			final List<Set<LogicalConstant>> setsOfConsts = new ArrayList<Set<LogicalConstant>>(
					signature.getTypes().size());
			final List<List<LogicalConstant>> potentialConstantSeqs = new LinkedList<List<LogicalConstant>>();
			// Create a the set of constants for each type in the signature.
			for (final Type type : signature.getTypes()) {
				final Set<LogicalConstant> consts = new HashSet<LogicalConstant>();
				for (final LogicalConstant constant : constants) {
					if (LogicLanguageServices.getTypeRepository()
							.generalizeType(constant.getType()).equals(type)) {
						consts.add(constant);
					}
				}
				setsOfConsts.add(consts);
			}

			for (final List<LogicalConstant> constantsList : CollectionUtils
					.cartesianProduct(setsOfConsts)) {
				potentialConstantSeqs
						.add(Collections.unmodifiableList(constantsList));
			}
			return potentialConstantSeqs;
		}
	}
}
