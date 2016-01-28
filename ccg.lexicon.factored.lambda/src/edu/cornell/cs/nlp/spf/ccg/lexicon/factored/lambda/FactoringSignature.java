package edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda;

import java.io.Serializable;
import java.util.List;

import edu.cornell.cs.nlp.spf.mr.language.type.Type;

/**
 * Factoring type signature.
 *
 * @author Yoav Artzi
 */
public class FactoringSignature implements Serializable {
	private static final long	serialVersionUID	= -875213556370792823L;
	private final int			hashCode;
	private final int			numAttributes;
	private final List<Type>	types;

	FactoringSignature(List<Type> types, int numAttributes) {
		this.numAttributes = numAttributes;
		assert types != null;
		assert numAttributes >= 0;
		this.types = types;
		this.hashCode = calcHashCode();
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
		final FactoringSignature other = (FactoringSignature) obj;
		if (numAttributes != other.numAttributes) {
			return false;
		}
		if (!types.equals(other.types)) {
			return false;
		}
		return true;
	}

	public int getNumAttributes() {
		return numAttributes;
	}

	public List<Type> getTypes() {
		return types;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numAttributes;
		result = prime * result + (types == null ? 0 : types.hashCode());
		return result;
	}

}
