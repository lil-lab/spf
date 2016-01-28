# v2.0
- Support for crossing and high order composition.
- Proper implementation of normal form parsing with Eisner (1996) NF.
- Hockenmaier and Bisk (2010) NF parsing. 
- IDs for generalized skolem terms.
- Improvement to GENLEX procedures + composite lexicon to easily combine GENLEX procedures.
- Better marking mechanism for CKY cells for constrained GENLEX parses.
- Data items for situated sentences.
- Improvements to situated (joint) inference.
- Model now automatically distinguishes between lexical and parse features.
- Numerous bug fixes and minor improvements.
- Store argument in Literal as array instead of List for better performance.
- Better performing LogicalExpression simplification. 
- Massive performance improvements.
- Variables now behave like in programming languages and "hide" each other. Variables names can be re-used and will be resolved correctly according to current scope.
- Support for syntactic attributes (e.g., S[dcl]) and single variable for agreement (e.g., S[x]\S[x]).
- CKY parsing derivations now store complete categories rather than only the logical forms (syntax included). 
- Multiple assertion checks -- NOTE: running with assertions on decreases performance significantly.
- Revise lexical factoring to work with syntactic attributes.
- spfdist: a simple distribution framework with no architecture assumptions (written with Mark Yatskar)
- Rename framework and packages
- Numerous other changes and optimizations

# v1.5.5
- Bug in identifying full parses when using unary rules.

# v1.5.4
- Introduce a readers system to read logical expressions from text. Also, make logical expression equality more flexible. Makes it easier to add new logical expression objects to hierarchy. 
- Various minor (but important) optimizations. 
- Add labeled single sentence data item with lexical entries. 
- Utility to parse a single sentence with a lexicon.
- Support for generalized function composition.
- Rewrite how the parser handles unary rules: more correct and better re-use of computations.

# v1.5.3
- Fix pruning in SingleSentence.
- Fixes and cleaning in LogicLanguageServices. Mostly relevant when working without a close ontology. 
- Keep the overflows away. Switch all exponentiated computations to log-space. 
- Fixed a small bug in example GeoQuery experiment.

# v1.5.2
- Revamp joint inference derivations and output: a derivation now includes all possible ways to create a certain result (i.e., it marginalizes over everything except the final result). Injective inference makes certain assumptions to provide a simpler interface. The general joint derivation doesn't support any dynamic programming for semantics evaluation. 
- Improve joint inference javadocs. 
- Add approximate/exact inference indication for inference outputs.
- Fix bug in TroveHashVector.
- Fix bug in LogicalConstant equality.


# v1.5.1
- Fixed inconsistencies in GeoQuery experiments.
- Release GeoQuery experiments with unification-based GENLEX. NOTE: these experiments don't re-create the results of Kwiatkowski et al. 2010/2011 as the implementation of GENLEX is different. 
- Move version info outside of README and build.xml.

# v1.5
- Better support for closed and open ontologies
	- Add option to allow using an ontology or not
	- Ontologies may now be closed or open
	- Explicit way to dynamically define new constants outside of .ont files
- Consolidate unit tests in a separate project
- Remove some unused code
- Removing stripping of redundant lambda operators
	- Simplifying to the canonical form doesn't strip redundant lambda operator. 
	- All implicit lambda operators are always present in the logical form. 
	- Converting a logical form to a string strips lambda operators for readability. 
	- Missing lambda operator in input logical expressions are added.
- Safety assertion in various constructors
- Improved support for joint inference and situated language
- GeoQuery example experiment without the experiment platform -- possibly a better way to get to know the code for some.
- Remove old storage mechanism and add support for saving models using serialization, including helper methods in the Model class.
- Removed concept of fixed lexical entry.
	- No distinction between fixed lexical entries and normal ones.
	- Initialization of seed entries is done using model initializers.
- Migrate all creators to inside the class they are creating (as internal classes).
- Fix bug with initializing the lexicon.
- Remove unused typing features.
- Better and more consistent typing validation and type consistency checks across the framework. 
- Remove support for unary parse rules. Unary operations are now supported only through overloading binary rules. See RuleSetBuilder.
- Hash vector improvements
	- Pairwise hash vector product.
	- Add method to apply a function all values in the hash vector.
- Attach Arxiv publication PDF and BIB files to source code.

# v1.4.1
- Cleaned up using of generics throughout the system. Generic classes should now compatible
- Better logging system - all logs are now public and can be controlled from outside SPF without editing SPF's code
- Javadocs for many generic classes
- Better abstraction in ExPlat's experiment hierarchy
- Better representations of situated data items
