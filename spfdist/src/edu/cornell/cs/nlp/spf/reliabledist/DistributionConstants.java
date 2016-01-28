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
package edu.cornell.cs.nlp.spf.reliabledist;

/**
 * @author Mark Yatskar
 */
public class DistributionConstants {

	public final static String	_ackid				= "__ACKID";
	public final static String	_awsacsess			= "__AWS_A";
	public final static String	_awssecret			= "__AWS_S";
	public final static String	_bucket				= "__BUCKET";
	public final static String	_classname			= "__CLASSNAME";
	public final static String	_command			= "__COMMAND";
	// public final static String DOWNLOAD = "DOWNLOAD"; //download to the
	// running directory.
	// public final static String DOWNLOADANDADD = "DOWNLOADANDADD"; //download
	// and add to the class loader
	// these are all thing that live in hashmaps. anything starting with "__" is
	// a system field
	public final static String	_failed				= "__FAILED";
	public final static String	_file				= "__FILE";
	public final static String	_free				= "__FREE";
	public final static String	_index				= "__INDEX";
	public final static String	_initcommand		= "__INITCOMMAND";
	public final static String	_message			= "__MESSAGE";
	public final static String	_stacktrace			= "__STACKTRACE";
	public final static String	_taskid				= "__TASKID";
	public final static String	AK					= "AK";			// basic
	// ak
	// command
	public final static String	AWSKEY				= "aws";
	public final static String	DOWNLOADFILE		= "download";
	public final static String	ENIVROMENT			= "ENVIROMENT";	// create
	// a new
	// enviroment
	public final static String	ERROR				= "ERROR";			// report
																		// and
																		// error.
	public final static String	INIT				= "INIT";
	public final static String	JARFILE				= "jarfile";		//
	// defualt logfile, i guess.
	public final static String	logfile				= "log.txt";
	public final static String	MODIFY_ENVIROMENT	= "MOD_ENVIROMENT"; // modify
	// the
	// current
	// enviroment
	public final static String	PING				= "PING";			// used
																		// for
																		// heart
																		// beat
	// config file commands
	public final static String	PORT				= "port";
	public final static String	RETURN				= "RETURN";		// return
	// these are all command string
	public final static String	SHUTDOWN			= "SHUTDOWN";
	// the
	// result
	// of a
	// computation.
	public final static String	SUMMARY				= "SUMMARY";		// request
																		// a
																		// summary
																		// (current
																		// not
																		// implemented)
	public final static String	WORK				= "WORK";			// submit
																		// some
																		// work
																		// to
																		// do.

}
