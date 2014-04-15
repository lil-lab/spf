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
package edu.uw.cs.lil.tiny.parser.ccg.joint.cky;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutputLogger;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Dump the underlying CKY chart into a file followed by all logical forms, each
 * with all the lexical entries that participate in any max-scoring parses
 * leading to it.
 * 
 * @author Yoav Artzi
 * @param <ESTEP>
 *            Formal semantics evaluation step.
 * @param <ERESULT>
 *            Formal semantics evaluation result.
 */
public class JointInferenceChartLogger<ESTEP, ERESULT> implements
		IJointOutputLogger<LogicalExpression, ESTEP, ERESULT> {
	public static final ILogger	LOG	= LoggerFactory
											.create(JointInferenceChartLogger.class);
	private final File			outputDir;
	
	public JointInferenceChartLogger(File outputDir) {
		this.outputDir = outputDir;
	}
	
	@Override
	public void log(IJointOutput<LogicalExpression, ERESULT> output,
			IJointDataItemModel<LogicalExpression, ESTEP> dataItemModel) {
		
		final Chart<LogicalExpression> chart = ((CKYParserOutput<LogicalExpression>) output
				.getBaseParserOutput()).getChart();
		try {
			final File file = new File(outputDir, String.format("chart-%d.txt",
					System.currentTimeMillis()));
			final FileWriter writer = new FileWriter(file);
			
			final Iterator<Cell<LogicalExpression>> iterator = chart
					.iterator(new Comparator<Cell<LogicalExpression>>() {
						
						@Override
						public int compare(Cell<LogicalExpression> o1,
								Cell<LogicalExpression> o2) {
							final int compare = Double.compare(
									o1.getPruneScore(), o2.getPruneScore());
							return compare == 0 ? o1.getCategory().toString()
									.compareTo(o2.getCategory().toString())
									: -compare;
						}
					});
			while (iterator.hasNext()) {
				final Cell<LogicalExpression> cell = iterator.next();
				writer.append(
						cell.toString(
								false,
								ListUtils.join(
										chart.getTokens().subList(
												cell.getStart(),
												cell.getEnd() + 1), " ")))
						.append(dataItemModel.getTheta().printValues(
								cell.computeMaxAvgFeaturesRecursively()))
						.append("\n");
			}
			
			writer.write("\n\n");
			for (final IDerivation<LogicalExpression> parse : CollectionUtils
					.sorted(output.getBaseParserOutput().getAllParses(),
							new Comparator<IDerivation<LogicalExpression>>() {
								
								@Override
								public int compare(
										IDerivation<LogicalExpression> o1,
										IDerivation<LogicalExpression> o2) {
									final int comp = Double.compare(
											o1.getScore(), o2.getScore());
									return comp == 0 ? o1
											.getSemantics()
											.toString()
											.compareTo(
													o2.getSemantics()
															.toString()) : comp;
								}
							})) {
				writer.write(String.format("[%.2f] %s\n", parse.getScore(),
						parse.getSemantics()));
				for (final LexicalEntry<LogicalExpression> entry : parse
						.getMaxLexicalEntries()) {
					writer.write(String.format(
							"\t[%f] %s [%s]\n",
							dataItemModel.score(entry),
							entry,
							dataItemModel.getTheta().printValues(
									dataItemModel.computeFeatures(entry))));
					writer.write(String.format(
							"\t%s\n",
							dataItemModel.getTheta().printValues(
									parse.getAverageMaxFeatureVector())));
				}
			}
			
			writer.close();
			LOG.info("Dumped chart to %s", file.getAbsolutePath());
		} catch (final IOException e) {
			LOG.error("Failed to write chart");
		}
	}
	
	public static class Creator<ESTEP, ERESULT> implements
			IResourceObjectCreator<JointInferenceChartLogger<ESTEP, ERESULT>> {
		
		private final String	name;
		
		public Creator() {
			this("chart.logger");
		}
		
		public Creator(String name) {
			this.name = name;
		}
		
		@Override
		public JointInferenceChartLogger<ESTEP, ERESULT> create(
				Parameters params, IResourceRepository repo) {
			return new JointInferenceChartLogger<ESTEP, ERESULT>(
					params.getAsFile("outputDir"));
		}
		
		@Override
		public String type() {
			return name;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					JointInferenceChartLogger.class)
					.setDescription(
							"Joint output logger that will dump CKY charts to logs")
					.addParam("outputDir", "dir",
							"Output dir to dump chart logs").build();
		}
		
	}
	
}
