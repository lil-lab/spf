package edu.uw.cs.lil.tiny.parser;

import edu.uw.cs.utils.filter.IFilter;

/**
 * Factory to create filters for parsing.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 * @param <O>
 *            Object to create the filter from.
 */
public interface IParsingFilterFactory<O, MR> {
	
	IFilter<MR> create(O object);
	
}
