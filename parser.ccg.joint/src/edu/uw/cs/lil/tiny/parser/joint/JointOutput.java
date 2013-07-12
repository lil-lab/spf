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
package edu.uw.cs.lil.tiny.parser.joint;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.utils.composites.Pair;

public class JointOutput<LF, ERESULT> implements IJointOutput<LF, ERESULT> {
	
	private final IParserOutput<LF>					baseParserOutput;
	private final List<IJointParse<LF, ERESULT>>	bestJointParses;
	private final List<IJointParse<LF, ERESULT>>	bestSuccessfulJointParses;
	private final long								inferenceTime;
	private final List<IJointParse<LF, ERESULT>>	jointParses;
	private final List<IJointParse<LF, ERESULT>>	successfulJointParses;
	
	public JointOutput(IParserOutput<LF> baseParserOutput,
			List<IJointParse<LF, ERESULT>> jointParses, long inferenceTime) {
		this.baseParserOutput = baseParserOutput;
		this.jointParses = jointParses;
		this.inferenceTime = inferenceTime;
		this.bestJointParses = findBestParses(jointParses);
		this.successfulJointParses = successfulOnly(jointParses);
		this.bestSuccessfulJointParses = findBestParses(successfulJointParses);
	}
	
	private static <Y, Z> List<IJointParse<Y, Z>> findBestParses(
			List<IJointParse<Y, Z>> all) {
		return findBestParses(all, null);
	}
	
	private static <Y, Z> List<IJointParse<Y, Z>> findBestParses(
			List<IJointParse<Y, Z>> all, Pair<Y, Z> label) {
		final List<IJointParse<Y, Z>> best = new LinkedList<IJointParse<Y, Z>>();
		double bestScore = -Double.MAX_VALUE;
		for (final IJointParse<Y, Z> p : all) {
			if ((label == null || ((label.first() == null || p.getResult()
					.first().equals(label.first())) && (label.second() == null || p
					.getResult().second().equals(label.second()))))) {
				if (p.getScore() == bestScore) {
					best.add(p);
				}
				if (p.getScore() > bestScore) {
					bestScore = p.getScore();
					best.clear();
					best.add(p);
				}
			}
		}
		return best;
	}
	
	private static <Y, Z> List<IJointParse<Y, Z>> successfulOnly(
			List<IJointParse<Y, Z>> parses) {
		final List<IJointParse<Y, Z>> successfulParses = new LinkedList<IJointParse<Y, Z>>();
		for (final IJointParse<Y, Z> parse : parses) {
			if (parse.getResult().second() != null) {
				successfulParses.add(parse);
			}
		}
		return successfulParses;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getAllParses() {
		return getAllParses(true);
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getAllParses(boolean includeFails) {
		if (includeFails) {
			return jointParses;
		} else {
			return successfulJointParses;
		}
	}
	
	@Override
	public IParserOutput<LF> getBaseParserOutput() {
		return baseParserOutput;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getBestParses() {
		return getBestParses(true);
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getBestParses(boolean includeFails) {
		if (includeFails) {
			return bestJointParses;
		} else {
			return bestSuccessfulJointParses;
		}
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getBestParsesFor(
			Pair<LF, ERESULT> label) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		final double score = -Double.MAX_VALUE;
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().equals(label)) {
				if (p.getScore() > score) {
					parses.clear();
					parses.add(p);
				} else if (p.getScore() == score) {
					parses.add(p);
				}
			}
		}
		return parses;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getBestParsesForY(LF partialLabel) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		final double score = -Double.MAX_VALUE;
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().first().equals(partialLabel)) {
				if (p.getScore() > score) {
					parses.clear();
					parses.add(p);
				} else if (p.getScore() == score) {
					parses.add(p);
				}
			}
		}
		return parses;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getBestParsesForZ(ERESULT partialLabel) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		final double score = -Double.MAX_VALUE;
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().second().equals(partialLabel)) {
				if (p.getScore() > score) {
					parses.clear();
					parses.add(p);
				} else if (p.getScore() == score) {
					parses.add(p);
				}
			}
		}
		return parses;
	}
	
	@Override
	public long getInferenceTime() {
		return inferenceTime;
	}
	
	public List<LexicalEntry<LF>> getMaxLexicalEntries(Pair<LF, ERESULT> label) {
		final List<LexicalEntry<LF>> result = new LinkedList<LexicalEntry<LF>>();
		for (final IJointParse<LF, ERESULT> p : findBestParses(jointParses,
				label)) {
			result.addAll(p.getMaxLexicalEntries());
		}
		return result;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getParsesFor(Pair<LF, ERESULT> label) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().equals(label)) {
				parses.add(p);
			}
		}
		return parses;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getParsesForY(LF partialLabel) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().first().equals(partialLabel)) {
				parses.add(p);
			}
		}
		return parses;
	}
	
	@Override
	public List<IJointParse<LF, ERESULT>> getParsesForZ(ERESULT partialLabel) {
		final List<IJointParse<LF, ERESULT>> parses = new LinkedList<IJointParse<LF, ERESULT>>();
		for (final IJointParse<LF, ERESULT> p : jointParses) {
			if (p.getResult().second().equals(partialLabel)) {
				parses.add(p);
			}
		}
		return parses;
	}
	
}
