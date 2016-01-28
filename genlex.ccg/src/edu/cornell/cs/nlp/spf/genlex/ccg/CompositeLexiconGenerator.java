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
package edu.cornell.cs.nlp.spf.genlex.ccg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.CompositeImmutableLexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelListener;

/**
 * @author Yoav Artzi
 * @param <DI>
 *            Training data item.
 * @param <MR>
 *            Meaning representation.
 * @param <MODEL>
 *            Inference model.
 */
public class CompositeLexiconGenerator<DI extends IDataItem<?>, MR, MODEL extends IModelImmutable<?, ?>>
		implements ILexiconGenerator<DI, MR, MODEL>, IModelListener<MR> {
	private static final long								serialVersionUID	= 5271965364968860866L;
	private final List<ILexiconGenerator<DI, MR, MODEL>>	genlexProcedures;

	public CompositeLexiconGenerator(
			List<ILexiconGenerator<DI, MR, MODEL>> genlexProcedures) {
		this.genlexProcedures = genlexProcedures;
	}

	@Override
	public ILexiconImmutable<MR> generate(DI dataItem, MODEL model,
			ICategoryServices<MR> categoryServices) {
		final List<ILexiconImmutable<MR>> lexicons = new ArrayList<ILexiconImmutable<MR>>(
				genlexProcedures.size());
		for (final ILexiconGenerator<DI, MR, MODEL> genlex : genlexProcedures) {
			lexicons.add(genlex.generate(dataItem, model, categoryServices));
		}
		return new CompositeImmutableLexicon<MR>(lexicons);
	}

	@Override
	public void init(MODEL model) {
		genlexProcedures.forEach(genlex -> genlex.init(model));
	}

	@Override
	public boolean isGenerated(LexicalEntry<MR> entry) {
		for (final ILexiconGenerator<DI, MR, MODEL> genlex : genlexProcedures) {
			if (genlex.isGenerated(entry)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void lexicalEntriesAdded(Collection<LexicalEntry<MR>> entries) {
		for (final LexicalEntry<MR> entry : entries) {
			lexicalEntryAdded(entry);
		}
	}

	@Override
	public void lexicalEntriesAdded(ILexicon<MR> entries) {
		lexicalEntriesAdded(entries.toCollection());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void lexicalEntryAdded(LexicalEntry<MR> entry) {
		for (final ILexiconGenerator<DI, MR, MODEL> genlex : genlexProcedures) {
			if (genlex instanceof IModelListener) {
				((IModelListener<MR>) genlex).lexicalEntryAdded(entry);
			}
		}
	}

	public static class Creator<DI extends IDataItem<?>, MR, MODEL extends IModelImmutable<?, ?>>
			implements
			IResourceObjectCreator<CompositeLexiconGenerator<DI, MR, MODEL>> {

		private String type;

		public Creator() {
			this("genlex.composite");
		}

		public Creator(String type) {
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompositeLexiconGenerator<DI, MR, MODEL> create(
				Parameters params, IResourceRepository repo) {
			final List<ILexiconGenerator<DI, MR, MODEL>> genlexProcedures = new LinkedList<ILexiconGenerator<DI, MR, MODEL>>();
			for (final String id : params.getSplit("procs")) {
				genlexProcedures
						.add((ILexiconGenerator<DI, MR, MODEL>) repo.get(id));
			}
			return new CompositeLexiconGenerator<DI, MR, MODEL>(
					genlexProcedures);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					CompositeLexiconGenerator.class)
							.setDescription("Composite GENLEX procedure")
							.addParam("procs", ILexiconGenerator.class,
									"List of GENLEX procedures to combine")
							.build();
		}

	}

}
