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
package edu.cornell.cs.nlp.spf.explat;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.InvalidParameterException;
import java.util.Map;

import edu.cornell.cs.nlp.spf.explat.resources.ResourceCreatorRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

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
		super(file, envParams, creatorRepo, file.getParentFile() == null
				? new File(".") : file.getParentFile());

		// Output directory.
		this.outputDir = globalParams.contains("outputDir")
				? globalParams.getAsFile("outputDir")
				: makeAbsolute(new File(
						ManagementFactory.getRuntimeMXBean().getName()));
		if (outputDir == null) {
			throw new IllegalArgumentException("Missing output dir");
		}
		// Create the directory, just to be on the safe side.
		outputDir.mkdir();

		// Init logging and output stream.
		final File globalLogFile = globalParams.contains("globalLog")
				? globalParams.getAsFile("globalLog") : null;
		if (globalLogFile == null) {
			Logger.DEFAULT_LOG = new Log(System.err);
			this.closeDefaultLog = false;
		} else {
			LOG.info("Logging to: %s", globalLogFile);
			Logger.DEFAULT_LOG = new Log(globalLogFile);
			this.closeDefaultLog = true;
		}
		Logger.setSkipPrefix(true);

		if (globalParams.contains("logLevel")) {
			final LogLevel setLevel = LogLevel
					.valueOf(globalParams.get("logLevel"));
			if (setLevel == null) {
				throw new InvalidParameterException(
						"Invalid log level: " + globalParams.get("logLevel"));
			} else {
				setLevel.set();
			}
		} else {
			LogLevel.setLogLevel(LogLevel.INFO);
		}

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
