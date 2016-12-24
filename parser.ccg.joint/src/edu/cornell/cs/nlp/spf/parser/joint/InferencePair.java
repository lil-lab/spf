package edu.cornell.cs.nlp.spf.parser.joint;

import edu.cornell.cs.nlp.spf.parser.IDerivation;

/**
 * Inference pair for joint derivation. Includes both the base derivation and
 * the
 * evaluation of the logical form at its root.
 *
 * @author Yoav Artzi
 *
 */
public class InferencePair<MR, ERESULT, PARSE extends IDerivation<MR>> {
	private final PARSE					baseDerivation;
	private final IEvaluation<ERESULT>	evaluationResult;

	public InferencePair(PARSE baseDerivation,
			IEvaluation<ERESULT> evaluationResult) {
		this.baseDerivation = baseDerivation;
		this.evaluationResult = evaluationResult;
	}

	public PARSE getBaseDerivation() {
		return baseDerivation;
	}

	public IEvaluation<ERESULT> getEvaluationResult() {
		return evaluationResult;
	}
}
