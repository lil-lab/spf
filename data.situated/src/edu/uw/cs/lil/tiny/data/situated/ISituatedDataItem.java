package edu.uw.cs.lil.tiny.data.situated;

import edu.uw.cs.lil.tiny.data.IDataItem;

/**
 * Data item for language in a situated environment.
 * 
 * @author Yoav Artzi
 * @param <LANG>
 *            Type of language.
 * @param <STATE>
 *            Situated state.
 */
public interface ISituatedDataItem<LANG, STATE> extends IDataItem<LANG> {
	
	STATE getState();
	
}
