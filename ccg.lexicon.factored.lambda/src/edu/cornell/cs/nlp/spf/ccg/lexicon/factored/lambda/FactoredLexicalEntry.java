package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda;

import java.util.Map;

import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;

public class FactoredLexicalEntry extends LexicalEntry<LogicalExpression> {

	private static final long		serialVersionUID	= -2547759640580678562L;
	private final Lexeme			lexeme;
	private final LexicalTemplate	template;

	FactoredLexicalEntry(TokenSeq tokens, Category<LogicalExpression> category,
			Lexeme lexeme, LexicalTemplate template, boolean dynamic,
			Map<String, String> properties) {
		super(tokens, category, dynamic, properties);
		this.lexeme = lexeme;
		this.template = template;
	}

	@Override
	public FactoredLexicalEntry cloneWithProperties(
			Map<String, String> newProperties) {
		return new FactoredLexicalEntry(getTokens(), getCategory(),
				lexeme.cloneWithProperties(newProperties),
				template.cloneWithProperties(newProperties), isDynamic(),
				newProperties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final FactoredLexicalEntry other = (FactoredLexicalEntry) obj;
		if (lexeme == null) {
			if (other.lexeme != null) {
				return false;
			}
		} else if (!lexeme.equals(other.lexeme)) {
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

	public Lexeme getLexeme() {
		return lexeme;
	}

	public LexicalTemplate getTemplate() {
		return template;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (lexeme == null ? 0 : lexeme.hashCode());
		result = prime * result + (template == null ? 0 : template.hashCode());
		return result;
	}

}