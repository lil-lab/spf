package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jregex.Pattern;
import jregex.Replacer;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LambdaWrapped;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Utility class to read logical expressions from strings.
 * 
 * @author Yoav Artzi
 */
public class LogicalExpressionReader {
	public static LogicalExpressionReader						INSTANCE				= new LogicalExpressionReader();
	public static final ILogger									LOG						= LoggerFactory
																								.create(LogicalExpressionReader.class);
	private static final Replacer								WHITE_SPACE_REPLACER	= new Replacer(
																								new Pattern(
																										"\\s+"),
																								" ");
	
	private final List<IReader<? extends LogicalExpression>>	readers					= new ArrayList<IReader<? extends LogicalExpression>>();
	
	private LogicalExpressionReader() {
	}
	
	static {
		// Register readers for all the basic types.
		register(new Lambda.Reader());
		register(new Literal.Reader());
		register(new Variable.Reader());
		register(new LogicalConstant.Reader());
	}
	
	/**
	 * Read a logical expression from a LISP formatted string.
	 */
	public static LogicalExpression from(String string) {
		return INSTANCE.read(string);
	}
	
	public static void register(IReader<? extends LogicalExpression> reader) {
		INSTANCE.readers.add(reader);
	}
	
	public static void reset() {
		INSTANCE = new LogicalExpressionReader();
	}
	
	public static void setInstance(LogicalExpressionReader reader) {
		LogicalExpressionReader.INSTANCE = reader;
	}
	
	/**
	 * Read a logical expression from a LISP formatted string.
	 */
	public LogicalExpression read(String string) {
		return read(string, LogicLanguageServices.getTypeRepository(),
				LogicLanguageServices.getTypeComparator());
		
	}
	
	/** {@see #read(String)} */
	private LogicalExpression read(String string,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		// Flatten the string. Replace all white space sequences with a single
		// space.
		final String flatString = WHITE_SPACE_REPLACER.replace(string);
		try {
			return LambdaWrapped.of(read(flatString,
					new HashMap<String, LogicalExpression>(), typeRepository,
					typeComparator));
		} catch (final RuntimeException e) {
			LOG.error("Logical expression syntax error: %s", flatString);
			throw e;
		}
	}
	
	/**
	 * Read a logical expression from a string.
	 * 
	 * @param string
	 *            LISP formatted string.
	 * @param mapping
	 *            Mapping of labels to logical expressions created during
	 *            parsing (for example, so we could re-use variables).
	 */
	protected LogicalExpression read(String string,
			Map<String, LogicalExpression> mapping,
			TypeRepository typeRepository, ITypeComparator typeComparator) {
		for (final IReader<? extends LogicalExpression> reader : readers) {
			if (reader.isValid(string)) {
				return reader.read(string, mapping, typeRepository,
						typeComparator, this);
			}
		}
		throw new IllegalArgumentException(
				"Invalid logical expression syntax: " + string);
	}
	
	public static interface IReader<LOGEXP extends LogicalExpression> extends
			IFilter<String> {
		
		LOGEXP read(String string, Map<String, LogicalExpression> mapping,
				TypeRepository typeRepository, ITypeComparator typeComparator,
				LogicalExpressionReader reader);
		
	}
	
}
