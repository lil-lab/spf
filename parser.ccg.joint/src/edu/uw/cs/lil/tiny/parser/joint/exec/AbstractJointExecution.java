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
package edu.uw.cs.lil.tiny.parser.joint.exec;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.exec.IExecution;
import edu.uw.cs.lil.tiny.parser.joint.IJointDerivation;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;

/**
 * Abstract execution wrapper for a joint derivation .Doesn't define the actual
 * execution output.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 * @param <RESULT>
 *            The final result {@link IExecution} will output.
 */
public abstract class AbstractJointExecution<MR, ERESULT, RESULT> implements
		IExecution<RESULT> {
	
	private final IJointDataItemModel<MR, ERESULT>	dataItemModel;
	protected final IJointDerivation<MR, ERESULT>	jointDerivation;
	
	public AbstractJointExecution(IJointDerivation<MR, ERESULT> jointParse,
			IJointDataItemModel<MR, ERESULT> dataItemModel) {
		this.jointDerivation = jointParse;
		this.dataItemModel = dataItemModel;
	}
	
	private static <MR, ERESULT> String lexToString(
			Iterable<LexicalEntry<MR>> lexicalEntries,
			IJointDataItemModel<MR, ERESULT> model) {
		final StringBuilder ret = new StringBuilder();
		ret.append("[LexEntries and scores:\n");
		for (final LexicalEntry<MR> entry : lexicalEntries) {
			ret.append("[").append(model.score(entry)).append("] ");
			ret.append(entry);
			ret.append(" [");
			ret.append(model.getTheta().printValues(
					model.computeFeatures(entry)));
			ret.append("]\n");
		}
		ret.append("]");
		return ret.toString();
	}
	
	@Override
	public double score() {
		return jointDerivation.getViterbiScore();
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	@Override
	public String toString(boolean verbose) {
		final StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("[S%.2f] %s", score(), getResult()));
		if (verbose) {
			sb.append('\n');
			sb.append(String.format("Features: %s\n", dataItemModel.getTheta()
					.printValues(jointDerivation.getMeanMaxFeatures())));
			sb.append(lexToString(jointDerivation.getMaxLexicalEntries(),
					dataItemModel));
		}
		
		return sb.toString();
	}
	
}
