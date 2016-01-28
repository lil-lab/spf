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
package edu.cornell.cs.nlp.spf.exec;

import java.io.Serializable;

import edu.cornell.cs.nlp.spf.data.IDataItem;

/**
 * Executor for creating {@link IExecOutput}. Such execution provide access to
 * the score and final result, but abstract the exact details of the execution
 * (e.g. lexical entries).
 *
 * @author Yoav Artzi
 * @param <DI>
 *            Data item for inference.
 * @param <RESULT>
 *            Final execution outcome.
 */
public interface IExec<DI extends IDataItem<?>, RESULT> extends Serializable {

	IExecOutput<RESULT> execute(DI dataItem);

	/**
	 * @param dataItem
	 * @param model
	 * @param sloppy
	 *            Allow sloppy interpretation of the input (e.g., skip words in
	 *            the input sentence)
	 * @return
	 */
	IExecOutput<RESULT> execute(DI dataItem, boolean sloppy);

}
