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
package edu.uw.cs.lil.tiny.explat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import edu.uw.cs.lil.tiny.explat.resources.ResourceCreatorRepository;
import edu.uw.cs.utils.assertion.Assert;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.Log;
import edu.uw.cs.utils.log.LogLevel;
import edu.uw.cs.utils.log.Logger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Adds basic logging setup over {@link ParameterizedExperiment}.
 * 
 * @author Yoav Artzi
 */
public abstract class LoggedExperiment extends ParameterizedExperiment {
	public static final ILogger	LOG	= LoggerFactory
											.create(LoggedExperiment.class);
	private final boolean		closeDefaultLog;
	protected final File		outputDir;
	
	public LoggedExperiment(File file, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo) throws IOException {
		this(new FileReader(file), envParams, creatorRepo,
				file.getParentFile() == null ? new File(".") : file
						.getParentFile());
	}
	
	public LoggedExperiment(Reader reader, Map<String, String> envParams,
			ResourceCreatorRepository creatorRepo, File rootDir)
			throws IOException {
		super(reader, envParams, creatorRepo, rootDir);
		
		// Output directory.
		this.outputDir = globalParams.contains("outputDir") ? globalParams
				.getAsFile("outputDir") : null;
		Assert.ifNull(outputDir);
		// Create the directory, just to be on the safe side.
		outputDir.mkdir();
		
		// Init logging and output stream.
		final File globalLogFile = globalParams.contains("globalLog") ? globalParams
				.getAsFile("globalLog") : null;
		if (globalLogFile == null) {
			Logger.DEFAULT_LOG = new Log(System.err);
			this.closeDefaultLog = false;
		} else {
			LOG.info("Logging to: %s", globalLogFile);
			Logger.DEFAULT_LOG = new Log(globalLogFile);
			this.closeDefaultLog = true;
		}
		Logger.setSkipPrefix(true);
		LogLevel.setLogLevel(LogLevel.INFO);
		
		// Log global parameters.
		LOG.info("Parameters:");
		for (final Pair<String, String> param : globalParams) {
			LOG.info("%s=%s", param.first(), param.second());
		}
		
	}
	
	public void end() {
		if (closeDefaultLog) {
			Logger.DEFAULT_LOG.close();
		}
	}
	
}
