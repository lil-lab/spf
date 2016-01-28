package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cornell.cs.nlp.spf.base.string.IStringFilter;
import edu.cornell.cs.nlp.spf.base.string.StubStringFilter;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.collections.MapUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Factored lexicon. Added lexical entries are factored and stored decomposed to
 * lexemes and lexical templates. See Kwiatkowski et al. 2011 for details.
 *
 * @author Yoav Artzi
 */
public class FactoredLexicon implements ILexicon<LogicalExpression> {
	public static final String									FACTORING_LEXICAL_ORIGIN	= "factoring";

	public static final ILogger									LOG							= LoggerFactory
			.create(FactoredLexicon.class);

	private static final long									serialVersionUID			= -9133601778066386561L;

	/**
	 * Lexemes are grouped by their strings, for quick indexing.
	 */
	private final Map<TokenSeq, Set<Lexeme>>					lexemes						= new HashMap<TokenSeq, Set<Lexeme>>();

	/**
	 * Maintain all lexemes indexed by type for quick access given template.
	 */
	private final Map<FactoringSignature, Set<Lexeme>>			lexemesByType				= new HashMap<FactoringSignature, Set<Lexeme>>();

	/**
	 * Templates are group by the types of their input arguments, for quick
	 * indexing.
	 */
	private final Map<FactoringSignature, Set<LexicalTemplate>>	templates					= new HashMap<FactoringSignature, Set<LexicalTemplate>>();

	public FactoredLexicon() {
	}

	public FactoredLexicon(Collection<Lexeme> lexemes,
			Collection<LexicalTemplate> templates) {
		for (final Lexeme lexeme : lexemes) {
			addLexeme(lexeme);
		}
		for (final LexicalTemplate template : templates) {
			addTemplate(template);
		}
	}

	private static FactoredLexicalEntry applyTemplate(LexicalTemplate template,
			Lexeme lexeme) {
		final Category<LogicalExpression> newCategory = template.apply(lexeme);
		if (newCategory == null) {
			return null;
		}
		return new FactoredLexicalEntry(lexeme.getTokens(), newCategory, lexeme,
				template, false, MapUtils.merge(lexeme.getProperties(),
						template.getProperties()));
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> add(
			LexicalEntry<LogicalExpression> entry) {
		final FactoredLexicalEntry factoredEntry = FactoringServices
				.factor(entry);
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		if (addLexeme(factoredEntry.getLexeme())) {
			added.addAll(getEntries(factoredEntry.getLexeme()));
		}
		if (addTemplate(factoredEntry.getTemplate())) {
			added.addAll(getEntries(factoredEntry.getTemplate()));
		}
		return added;
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			Collection<LexicalEntry<LogicalExpression>> entries) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final LexicalEntry<LogicalExpression> lex : entries) {
			added.addAll(add(lex));
		}
		return added;
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addAll(
			ILexicon<LogicalExpression> lexicon) {
		final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
		if (lexicon instanceof FactoredLexicon) {
			final FactoredLexicon flex = (FactoredLexicon) lexicon;

			// Add lexemes
			for (final Set<Lexeme> set : flex.lexemes.values()) {
				for (final Lexeme lexeme : set) {
					if (addLexeme(lexeme)) {
						added.addAll(getEntries(lexeme));
					}
				}
			}

			// Add templates
			for (final Set<LexicalTemplate> set : flex.templates.values()) {
				for (final LexicalTemplate template : set) {
					if (addTemplate(template)) {
						added.addAll(getEntries(template));
					}
				}
			}
			return added;
		} else {
			return addAll(lexicon.toCollection());
		}
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			ICategoryServices<LogicalExpression> categoryServices,
			String origin) {
		return addEntriesFromFile(file, new StubStringFilter(),
				categoryServices, origin);
	}

	/**
	 * Read entries from a file, one per line, of the form
	 *
	 * <pre>
	 *  Tokens  :-  Cat
	 * </pre>
	 */
	@Override
	public Set<LexicalEntry<LogicalExpression>> addEntriesFromFile(File file,
			IStringFilter textFilter,
			ICategoryServices<LogicalExpression> categoryServices,
			String origin) {
		try {
			final Set<LexicalEntry<LogicalExpression>> added = new HashSet<LexicalEntry<LogicalExpression>>();
			int lineCounter = 0;
			try (final BufferedReader in = new BufferedReader(
					new FileReader(file))) {
				String line;
				// For each line in the file
				while ((line = in.readLine()) != null) {
					++lineCounter;
					line = line.trim();
					// Ignore blank lines and comments
					if (!line.equals("") && !line.startsWith("//")) {
						added.addAll(add(LexicalEntry.read(line, textFilter,
								categoryServices, origin)));
					}
				}
			} catch (final RuntimeException e) {
				throw new RuntimeException(String.format(
						"Reading of input file %s failed at line %d",
						file.getName(), lineCounter), e);
			}
			return added;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean contains(Lexeme lexeme) {
		final TokenSeq tokens = lexeme.getTokens();
		return lexemes.containsKey(tokens)
				&& lexemes.get(tokens).contains(lexeme);
	}

	@Override
	public boolean contains(LexicalEntry<LogicalExpression> entry) {
		final FactoredLexicalEntry factoring = FactoringServices.factor(entry);
		final Set<Lexeme> lexemeSet = lexemes
				.get(factoring.getLexeme().getTokens());
		final Set<LexicalTemplate> templateSet = templates
				.get(factoring.getTemplate().getSignature());

		return lexemeSet != null && templateSet != null
				&& lexemeSet.contains(factoring.getLexeme())
				&& templateSet.contains(factoring.getTemplate());
	}

	public boolean contains(LexicalTemplate template) {
		final FactoringSignature signature = template.getSignature();
		return templates.containsKey(signature)
				&& templates.get(signature).contains(template);
	}

	@Override
	public Iterator<FactoredLexicalEntry> get(TokenSeq tokens) {
		final Set<Lexeme> tokenLexemes = lexemes.get(tokens);
		if (tokenLexemes == null) {
			return Collections.emptyIterator();
		}

		// Create an iterator that iterates over all lexemes and for each lexeme
		// iterates over all matching templates to generated lexical entries.
		return new Iterator<FactoredLexicalEntry>() {
			private Lexeme						currentLexeme		= null;
			private FactoredLexicalEntry		nextEntry			= null;

			private Iterator<LexicalTemplate>	templateIterator	= null;

			final Iterator<Lexeme>				lexemeIterator		= tokenLexemes
					.iterator();

			@Override
			public boolean hasNext() {
				if (nextEntry == null) {
					if (!loadNextEntry()) {
						return false;
					}
				}
				return true;
			}

			@Override
			public FactoredLexicalEntry next() {
				if (nextEntry == null) {
					loadNextEntry();
				}

				final FactoredLexicalEntry next = nextEntry;
				nextEntry = null;
				return next;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private boolean loadNextEntry() {
				do {
					while (templateIterator != null
							&& templateIterator.hasNext()) {
						nextEntry = applyTemplate(templateIterator.next(),
								currentLexeme);
						if (nextEntry != null) {
							return true;
						}
					}
				} while (loadNextLexeme());

				return false;
			}

			/**
			 * Assumes the current template iterator is exhausted, tries to load
			 * the next lexeme, if available, and its template iterator.
			 *
			 * @return <code>false</code> if the iterator is completely
			 *         exhausted, <code>true</code> otherwise.
			 */
			private boolean loadNextLexeme() {
				while (lexemeIterator.hasNext()) {
					currentLexeme = lexemeIterator.next();
					final Set<LexicalTemplate> templateSet = templates
							.get(currentLexeme.getSignature());
					if (templateSet != null && !templateSet.isEmpty()) {
						templateIterator = templateSet.iterator();
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	public boolean retainAll(
			Collection<LexicalEntry<LogicalExpression>> toKeepEntries) {
		final FactoredLexicon factoredLexicon = new FactoredLexicon();
		factoredLexicon.addAll(toKeepEntries);
		return retainAll(factoredLexicon);
	}

	@Override
	public boolean retainAll(ILexicon<LogicalExpression> lexicon) {
		if (lexicon instanceof FactoredLexicon) {
			// Case factored lexicon, so should remove all lexemes and templates
			// it doesn't include
			final FactoredLexicon factoredLexicon = (FactoredLexicon) lexicon;
			boolean somethingRemoved = false;

			// Remove lexemes from main lexemes mapping (log as needed).
			final Iterator<Entry<TokenSeq, Set<Lexeme>>> lexemeIterator = lexemes
					.entrySet().iterator();
			while (lexemeIterator.hasNext()) {
				final Entry<TokenSeq, Set<Lexeme>> lexemeEntry = lexemeIterator
						.next();
				if (factoredLexicon.lexemes.containsKey(lexemeEntry.getKey())) {
					LOG.debug(() -> {
						// Case string seq. known, remove lexemes not present.
						final Set<Lexeme> retainSet = factoredLexicon.lexemes
								.get(lexemeEntry.getKey());
						for (final Lexeme lexeme : lexemeEntry.getValue()) {
							if (!retainSet.contains(lexeme)) {
								LOG.debug("Removing lexeme: %s", lexeme);
							}
						}
					});
					somethingRemoved = lexemeEntry.getValue()
							.retainAll(factoredLexicon.lexemes
									.get(lexemeEntry.getKey()))
							|| somethingRemoved;
				} else {
					// Case this string sequence is not present, remove all its
					// lexemes
					LOG.debug(() -> {
						for (final Lexeme lexeme : lexemeEntry.getValue()) {
							LOG.debug("Removing lexeme: %s", lexeme);
						}
					});
					lexemeIterator.remove();
					somethingRemoved = true;
				}
			}
			// Remove lexemes from signature mapping (removed lexemes already
			// logged, so not necessary).
			final Iterator<Entry<FactoringSignature, Set<Lexeme>>> lexemeTypeIterator = lexemesByType
					.entrySet().iterator();
			while (lexemeTypeIterator.hasNext()) {
				final Entry<FactoringSignature, Set<Lexeme>> lexemeEntry = lexemeTypeIterator
						.next();
				if (factoredLexicon.lexemesByType
						.containsKey(lexemeEntry.getKey())) {
					// Case signature known, remove lexemes not present.
					somethingRemoved = lexemeEntry.getValue()
							.retainAll(factoredLexicon.lexemesByType
									.get(lexemeEntry.getKey()))
							|| somethingRemoved;
				} else {
					// Case unknown signature, remove all its lexemes.
					lexemeIterator.remove();
					somethingRemoved = true;
				}
			}

			// Remove templates
			final Iterator<Entry<FactoringSignature, Set<LexicalTemplate>>> templateIterator = templates
					.entrySet().iterator();
			while (templateIterator.hasNext()) {
				final Entry<FactoringSignature, Set<LexicalTemplate>> templateEntry = templateIterator
						.next();
				if (factoredLexicon.templates
						.containsKey(templateEntry.getKey())) {
					// Case type signature present, remove all templates not
					// present
					LOG.debug(() -> {
						final Set<LexicalTemplate> retainSet = factoredLexicon.templates
								.get(templateEntry.getKey());
						for (final LexicalTemplate template : templateEntry
								.getValue()) {
							if (!retainSet.contains(template)) {
								LOG.debug("Removing template: %s", template);
							}
						}
					});
					somethingRemoved = templateEntry.getValue()
							.retainAll(factoredLexicon.templates
									.get(templateEntry.getKey()))
							|| somethingRemoved;
				} else {
					// Case type signature not present, remove all templates
					LOG.debug(() -> {
						for (final LexicalTemplate template : templateEntry
								.getValue()) {
							LOG.debug("Removing template: %s", template);
						}
					});
					templateIterator.remove();
					somethingRemoved = true;
				}
			}

			return somethingRemoved;
		} else {
			return retainAll(lexicon.toCollection());
		}
	}

	@Override
	public int size() {
		int size = 0;
		for (final Entry<FactoringSignature, Set<Lexeme>> signatureLexemes : lexemesByType
				.entrySet()) {
			size += templates
					.getOrDefault(signatureLexemes.getKey(),
							Collections.emptySet())
					.size() * signatureLexemes.getValue().size();
		}
		return size;
	}

	/**
	 * WARNING: really inefficient to call this... should avoid at all costs if
	 * lexicon is large.
	 *
	 * @return
	 */
	@Override
	public Collection<LexicalEntry<LogicalExpression>> toCollection() {
		final Set<LexicalEntry<LogicalExpression>> result = new HashSet<LexicalEntry<LogicalExpression>>();
		for (final Entry<FactoringSignature, Set<Lexeme>> signatureLexemes : lexemesByType
				.entrySet()) {
			if (templates.containsKey(signatureLexemes.getKey())) {
				for (final LexicalTemplate template : templates
						.get(signatureLexemes.getKey())) {
					for (final Lexeme lexeme : signatureLexemes.getValue()) {
						final LexicalEntry<LogicalExpression> newLex = applyTemplate(
								template, lexeme);
						if (newLex != null) {
							result.add(newLex);
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {

		final StringBuilder ret = new StringBuilder();
		ret.append("Lexemes:\n");
		for (final Entry<TokenSeq, Set<Lexeme>> entry : lexemes.entrySet()) {
			ret.append(entry.getKey());
			ret.append("=");
			ret.append(entry.getValue());
			ret.append("\n");
		}
		ret.append("Templates:\n");
		for (final Entry<FactoringSignature, Set<LexicalTemplate>> entry : templates
				.entrySet()) {
			ret.append(entry.getValue());
			ret.append("\n");
		}
		return ret.toString();
	}

	private boolean addLexeme(Lexeme lexeme) {
		final Set<Lexeme> lexemeSet = lexemes
				.computeIfAbsent(lexeme.getTokens(), tokens -> new HashSet<>());
		final boolean added;
		try {
			added = lexemeSet.add(lexeme);
		} catch (final RuntimeException e) {
			LOG.error("Exception (lexemeSet=%d)", lexemeSet.size());
			LOG.error(e);
			throw e;
		}

		if (added) {
			// Update lexeme indexing by type signature.
			final FactoringSignature typeSignature = lexeme.getSignature();
			final Set<Lexeme> typeSet = lexemesByType.computeIfAbsent(
					typeSignature, signature -> new HashSet<>());
			typeSet.add(lexeme);
		}

		return added;
	}

	private boolean addTemplate(LexicalTemplate template) {
		final Set<LexicalTemplate> templateSet = templates.computeIfAbsent(
				template.getSignature(), signture -> new HashSet<>());
		return templateSet.add(template);
	}

	/**
	 * Get all {@link LexicalEntry}s for a given {@link Lexeme}.
	 */
	private Set<LexicalEntry<LogicalExpression>> getEntries(Lexeme lexeme) {
		final Set<LexicalEntry<LogicalExpression>> entries = new HashSet<LexicalEntry<LogicalExpression>>();
		final FactoringSignature signature = lexeme.getSignature();
		if (templates.containsKey(signature)) {
			for (final LexicalTemplate template : templates.get(signature)) {
				final FactoredLexicalEntry entry = applyTemplate(template,
						lexeme);
				if (entry != null) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	/**
	 * Get all {@link LexicalEntry}s for a given {@link LexicalTemplate}.
	 */
	private Set<LexicalEntry<LogicalExpression>> getEntries(
			LexicalTemplate template) {
		final Set<LexicalEntry<LogicalExpression>> entries = new HashSet<LexicalEntry<LogicalExpression>>();
		final FactoringSignature typeSignature = template.getSignature();
		if (lexemesByType.containsKey(typeSignature)) {
			for (final Lexeme lexeme : lexemesByType.get(typeSignature)) {
				final FactoredLexicalEntry entry = applyTemplate(template,
						lexeme);
				if (entry != null) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	public static class Creator
			implements IResourceObjectCreator<FactoredLexicon> {

		@SuppressWarnings("unchecked")
		@Override
		public FactoredLexicon create(Parameters params,
				IResourceRepository repo) {
			final FactoredLexicon lexicon = new FactoredLexicon();
			// Add entries from files.
			for (final File file : params.getAsFiles("files")) {
				lexicon.addEntriesFromFile(file,
						(ICategoryServices<LogicalExpression>) repo
								.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
						params.get("origin"));
			}
			// Add entries from other lexicons.
			for (final String id : params.getSplit("base")) {
				lexicon.addAll((ILexicon<LogicalExpression>) repo.get(id));
			}

			return lexicon;
		}

		@Override
		public String type() {
			return "lexicon.factored";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), FactoredLexicon.class)
					.addParam("files", File.class,
							"List of files to read entries from")
					.setDescription(
							"Lexicon that contains factored entries. Entries are factored as they are added. The lexicon contains all entries that can be generated by its templates and lexeme")
					.build();
		}

	}

}
