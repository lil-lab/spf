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
package edu.cornell.cs.nlp.spf.parser.ccg.cky.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IOutputLogger;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.CKYParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Chart;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Dump the underlying CKY chart into a file followed by all logical forms, each
 * with all the lexical entries that participate in any max-scoring parses
 * leading to it.
 *
 * @author Yoav Artzi
 */
public class ChartLogger<MR> implements IOutputLogger<MR> {
	public static final ILogger LOG = LoggerFactory.create(ChartLogger.class);

	private static final long serialVersionUID = -4161484956605304553L;

	private final File		outputDir;
	private final String	prefix;
	private final boolean	viterbi;

	public ChartLogger(File outputDir, String prefix, boolean viterbi) {
		this.outputDir = outputDir;
		this.prefix = prefix;
		this.viterbi = viterbi;
	}

	@Override
	public void log(IParserOutput<MR> output, IDataItemModel<MR> dataItemModel,
			String tag) {
		if (!outputDir.isDirectory()) {
			LOG.warn("Failed to write chart (directory missing)");
			return;
		}

		if (!(output instanceof CKYParserOutput)) {
			LOG.error(
					"Failed to write chart, output is not a CKY parser output");
			return;
		}

		final Chart<MR> chart = ((CKYParserOutput<MR>) output).getChart();
		try {
			final long startTime = System.currentTimeMillis();
			final File file = new File(outputDir,
					String.format("%s%s.chart",
							prefix == null ? "" : prefix + "-",
							tag == null ? "" : tag));
			try (final FileWriter writer = new FileWriter(file)) {
				writer.append(chart.toString(true, viterbi,
						dataItemModel.getTheta()));
				writer.write("\n\n");
				for (final IDerivation<MR> parse : CollectionUtils
						.sorted(output.getAllDerivations(), (o1, o2) -> {
							final int comp = Double.compare(o2.getScore(),
									o1.getScore());
							return comp == 0 ? o2.getCategory().toString()
									.compareTo(o1.getCategory().toString())
									: comp;
						})) {
					writer.write(String.format("[%.2f] %s\n", parse.getScore(),
							parse.getCategory()));
					for (final LexicalEntry<MR> entry : parse
							.getMaxLexicalEntries()) {
						writer.write(String.format("\t[%f] %s [%s]\n",
								dataItemModel.score(entry), entry,
								dataItemModel.getTheta().printValues(
										dataItemModel.computeFeatures(entry))));
					}
					writer.write(String.format("\tRules: %s\n",
							ListUtils.join(parse.getMaxRulesUsed(), ", ")));
					writer.write(String.format("\tFeatures: %s\n",
							dataItemModel.getTheta().printValues(
									parse.getAverageMaxFeatureVector())));
				}
			}
			LOG.info("Dumped chart to %s (%.3fsec)", file.getAbsolutePath(),
					(System.currentTimeMillis() - startTime) / 1000.0);
		} catch (final IOException e) {
			LOG.error("Failed to write chart");
		}
	}

	public static class Creator<MR>
			implements IResourceObjectCreator<ChartLogger<MR>> {

		private final String name;

		public Creator() {
			this("logger.chart");
		}

		public Creator(String name) {
			this.name = name;
		}

		@Override
		public ChartLogger<MR> create(Parameters params,
				IResourceRepository repo) {
			return new ChartLogger<MR>(params.getAsFile("outputDir"),
					params.get("prefix", null),
					params.getAsBoolean("viterbi", false));
		}

		@Override
		public String type() {
			return name;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), ChartLogger.class)
					.setDescription(
							"Output logger that will dump CKY charts to logs")
					.addParam("outputDir", "dir",
							"Output dir to dump chart logs")
					.addParam("viterbi", Boolean.class,
							"Only log viterbi steps for each cell in the chart (default: false)")
					.build();
		}

	}

}
