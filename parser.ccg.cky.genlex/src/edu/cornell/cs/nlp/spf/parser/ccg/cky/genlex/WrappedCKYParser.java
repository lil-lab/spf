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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.genlex;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.base.concurrency.Shutdownable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.AbstractCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParser;
import edu.cornell.cs.nlp.spf.parser.graph.IGraphParserOutput;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Wrapper for CKY parser to use a marked cells.
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item.
 * @param <MR>
 *            Meaning representation.
 */
public class WrappedCKYParser<DI extends Sentence, MR>
		implements IGraphParser<DI, MR>, Shutdownable {
	public static final ILogger				LOG					= LoggerFactory
			.create(WrappedCKYParser.class);
	/**
	 *
	 */
	private static final long				serialVersionUID	= 3697017550457795710L;
	private final AbstractCKYParser<DI, MR>	ckyParser;
	private final boolean					useMarkingForPruning;

	public WrappedCKYParser(AbstractCKYParser<DI, MR> ckyParser,
			boolean useMarkingForPruning) {
		this.ckyParser = ckyParser;
		this.useMarkingForPruning = useMarkingForPruning;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		if (ckyParser instanceof Shutdownable) {
			return ((Shutdownable) ckyParser).awaitTermination(timeout, unit);
		}
		return true;
	}

	@Override
	public boolean isShutdown() {
		if (ckyParser instanceof Shutdownable) {
			return ((Shutdownable) ckyParser).isShutdown();
		}
		return true;
	}

	@Override
	public boolean isTerminated() {
		if (ckyParser instanceof Shutdownable) {
			return ((Shutdownable) ckyParser).isTerminated();
		}
		return true;
	}

	@Override
	public IGraphParserOutput<MR> parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter, IDataItemModel<MR> model,
			boolean allowWordSkipping, ILexiconImmutable<MR> tempLexicon,
			Integer beamSize) {
		LOG.debug("CKY parsing with marked cells.");
		return ckyParser.parse(dataItem, pruningFilter, model,
				allowWordSkipping, tempLexicon, beamSize,
				new MarkedCellFactory<MR>(
						dataItem.getSample().getTokens().size(),
						useMarkingForPruning));
	}

	@Override
	public void shutdown() {
		if (ckyParser instanceof Shutdownable) {
			((Shutdownable) ckyParser).shutdown();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		if (ckyParser instanceof Shutdownable) {
			return ((Shutdownable) ckyParser).shutdownNow();
		}
		return Collections.emptyList();
	}

}
