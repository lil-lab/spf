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
import edu.uw.cs.utils.filter.IFilter;

public class GenericJointOutput<MR, ERESULT, PARSE extends IJointParse<MR, ERESULT>>
		implements IJointOutput<MR, ERESULT> {
	
	private final IParserOutput<MR>	baseParserOutput;
	private final List<PARSE>		bestJointParses;
	private final List<PARSE>		bestSuccessfulJointParses;
	private final long				inferenceTime;
	private final List<PARSE>		jointParses;
	private final List<PARSE>		successfulJointParses;
	
	public GenericJointOutput(IParserOutput<MR> baseParserOutput,
			List<PARSE> jointParses, long inferenceTime) {
		this.baseParserOutput = baseParserOutput;
		this.jointParses = jointParses;
		this.inferenceTime = inferenceTime;
		this.bestJointParses = findBestParses(jointParses);
		this.successfulJointParses = successfulOnly(jointParses);
		this.bestSuccessfulJointParses = findBestParses(successfulJointParses);
	}
	
	@Override
	public List<PARSE> getAllParses() {
		return getAllParses(true);
	}
	
	@Override
	public List<PARSE> getAllParses(boolean includeFails) {
		if (includeFails) {
			return jointParses;
		} else {
			return successfulJointParses;
		}
	}
	
	@Override
	public IParserOutput<MR> getBaseParserOutput() {
		return baseParserOutput;
	}
	
	@Override
	public List<PARSE> getBestParses() {
		return getBestParses(true);
	}
	
	@Override
	public List<PARSE> getBestParses(boolean includeFails) {
		if (includeFails) {
			return bestJointParses;
		} else {
			return bestSuccessfulJointParses;
		}
	}
	
	@Override
	public List<PARSE> getBestParsesFor(final Pair<MR, ERESULT> label) {
		return getMaxParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return label.equals(e);
			}
		});
	}
	
	@Override
	public List<PARSE> getBestParsesForY(final MR partialLabel) {
		return getMaxParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return partialLabel.equals(e.first());
			}
		});
	}
	
	@Override
	public List<PARSE> getBestParsesForZ(final ERESULT partialLabel) {
		return getMaxParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return partialLabel.equals(e.second());
			}
		});
	}
	
	@Override
	public long getInferenceTime() {
		return inferenceTime;
	}
	
	public List<LexicalEntry<MR>> getMaxLexicalEntries(Pair<MR, ERESULT> label) {
		final List<LexicalEntry<MR>> result = new LinkedList<LexicalEntry<MR>>();
		for (final PARSE p : findBestParses(jointParses, label)) {
			result.addAll(p.getMaxLexicalEntries());
		}
		return result;
	}
	
	@Override
	public List<PARSE> getMaxParses(IFilter<Pair<MR, ERESULT>> filter) {
		final List<PARSE> parses = new LinkedList<PARSE>();
		final double score = -Double.MAX_VALUE;
		for (final PARSE p : jointParses) {
			if (filter.isValid(p.getResult())) {
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
	public List<PARSE> getParses(IFilter<Pair<MR, ERESULT>> filter) {
		final List<PARSE> parses = new LinkedList<PARSE>();
		for (final PARSE p : jointParses) {
			if (filter.isValid(p.getResult())) {
				parses.add(p);
			}
		}
		return parses;
	}
	
	@Override
	public List<PARSE> getParsesFor(final Pair<MR, ERESULT> label) {
		return getParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return label.equals(e);
			}
		});
	}
	
	@Override
	public List<PARSE> getParsesForY(final MR partialLabel) {
		return getParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return e.first().equals(partialLabel);
			}
		});
	}
	
	@Override
	public List<PARSE> getParsesForZ(final ERESULT partialLabel) {
		return getParses(new IFilter<Pair<MR, ERESULT>>() {
			
			@Override
			public boolean isValid(Pair<MR, ERESULT> e) {
				return e.second().equals(partialLabel);
			}
		});
	}
	
	private List<PARSE> findBestParses(List<PARSE> all) {
		return findBestParses(all, null);
	}
	
	private List<PARSE> findBestParses(List<PARSE> all, Pair<MR, ERESULT> label) {
		final List<PARSE> best = new LinkedList<PARSE>();
		double bestScore = -Double.MAX_VALUE;
		for (final PARSE p : all) {
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
	
	private List<PARSE> successfulOnly(List<PARSE> parses) {
		final List<PARSE> successfulParses = new LinkedList<PARSE>();
		for (final PARSE parse : parses) {
			if (parse.getResult().second() != null) {
				successfulParses.add(parse);
			}
		}
		return successfulParses;
	}
	
}
