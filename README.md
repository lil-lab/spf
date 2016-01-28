Title: Cornell SPF - README
Author: Yoav Artzi
Affiliation: Computer Science, Cornell University
Web: http://yoavartzi.com/spf

# [_**Cornell SPF**_](http://yoavartzi.com/spf) - Cornell Semantic Parsing Framework 

[TOC]

## Authors 

**Developed and maintained by** [Yoav Artzi](http://yoavartzi.com)  
**Past contributors:** [Luke Zettlemoyer](http://homes.cs.washington.edu/~lsz/), Tom Kwiatkowski, [Kenton Lee](http://homes.cs.washington.edu/~kentonl/)

## Tutorial

See our ACL 2013 tutorial for an introduction to semantic parsing with Combinatory Categorial Grammars (CCGs). The slides and videos are available [here](http://yoavartzi.com/tutorial).

## Discussion Group

Please post all technical questions and inquiries in our the [https://groups.google.com/d/forum/cornellspf](Cornell SPF Google Group).

## Attribution

When using Cornell SPF, please acknowledge it by citing:

Artzi, Y., & Zettlemoyer, L. (2013). UW SPF: The University of Washington Semantic Parsing Framework.

[**Bibtex:**](http://yoavartzi.com/pub/az-arxiv.2013.bib)

    @misc{Artzi:13spf,
	    Author = {Yoav Artzi and Luke Zettlemoyer},
	    Title = {{UW SPF: The University of Washington Semantic Parsing Framework}},
	    Year = {2013},
	    Eprint = {arXiv:1311.3011},
    }

The article and bib file are both attached to the source code. When using specific algorithms please cite the appropriate work (see below).

### Validation-based learning, joint inference and coarse-to-fine lexical generation

Yoav Artzi and Luke Zettlemoyer. [Weakly Supervised Learning of Semantic Parsers for Mapping Instructions to Actions](http://yoavartzi.com/pub/az-tacl.2013.pdf). In Transactions of the Association for Computational Linguistics (TACL), 2013.

### Loss-sensitive learning

Yoav Artzi and Luke Zettlemoyer. [Bootstrapping Semantic Parsers from Conversations](http://yoavartzi.com/pub/az-emnlp.2011.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2011.

### Unification-based GENLEX

Tom Kwiatkowski, Luke Zettlemoyer, Sharon Goldwater, and Mark Steedman. [Inducing Probabilistic CCG Grammars from Logical Form with Higher-order Unification](http://homes.cs.washington.edu/~lsz/papers/kzgs-emnlp2010.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2010.

### Factored lexicons

Tom Kwiatkowski, Luke Zettlemoyer, Sharon Goldwater, and Mark Steedman. [Lexical Generalization in CCG Grammar Induction for Semantic Parsing](http://homes.cs.washington.edu/~lsz/papers/kzgs-emnlp2011.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2011.

### Template-based GENLEX

Luke Zettlemoyer and Michael Collins. [Online Learning of Relaxed CCG Grammars for Parsing to Logical Form](http://homes.cs.washington.edu/~lsz/papers/zc-emnlp07.pdf). In Proceedings of the Joint Conference on Empirical Methods in Natural Language Processing and Computational Natural Language Learning (EMNLP-CoNLL), 2007.

Luke Zettlemoyer and Michael Collins. [Learning to Map Sentences to Logical Form: Structured Classification with Probabilistic Categorial Grammars](http://homes.cs.washington.edu/~lsz/papers/zc-uai05.pdf). In Proceedings of the Twenty First Conference on Uncertainty in Artificial Intelligence (UAI), 2005.

## Documentation

**We are constantly updating this section. Most of the documentation is within the code in the form of Javadoc.**

### Building

To compile SPF use: `ant dist`. The output JAR file will be in the `dist` directory. You can also download the compiled JAR file from the [downloads](https://bitbucket.org/yoavartzi/spf/downloads) section.

### Running example experiments

The framework contains an example experiment using the GeoQuery corpus. To use development fold 0 for testing, and training on the other folds, use:
``java -jar dist/spf-1.4.jar geoquery/experiments/template/dev.cross/dev.fold0.exp``  
The log and output files are written to a newly generated directory in the experiment directory:
``geoquery/experiments/template/dev.cross/``

View the .exp file and see how it defines arguments and how it includes them from other files. Another critical point of entry is the class ``edu.cornell.cs.lil.tiny.geoquery.GeoMain``. The experiments platform (ExPlat) is reviewed [below][explat].

### Working with the Code

The code is divided into many projects with dependencies between them. You can work with the code with in editor and build  it with the accompanying ANT script. However, we recommend using Eclipse. Each directory is an Eclipse project and can be easily imported into Eclipse. To do so select ``Import`` from the ``File`` menu and choose ``Existing Projects into Workspace``. The ``Root Directory`` should be the code directory and all projects should be selected by default. The dependencies will be imported automatically. To successfully build SPF in Eclipse you will need to set the classpath variable ``TINY_REPO`` to the code directory. To so go to ``Preferences`` -> ``Java`` -> ``Build Path`` -> ``Classpath Variables``, add a new variable with the name `TINY_REPO` and a folder value that points to the repository location. 

### Getting to Know the Code

There are two ways to use SPF. The first is by simply calling the code in your own classes. To see how a complete experiment can be set up programmatically please see  `edu.cornell.cs.lil.tiny.geoquery.GeoExpSimple`. The `main(String[])` method in this class initializes the framework, instantiates all required objects for inference, learning and testing and launches the appropriate jobs in order. A slightly more robust way to conduct experiments is provided by the [ExPlat][explat] internal experiments framework. The previously mentioned example experiments are using ExPlat.

### Logging in SPF

SPF's logging system should be initialized prior to using the framework. Including the default output stream (e.g., `Logger.DEFAULT_LOG = new Log(System.err);`) and log threshold (e.g., `LogLevel.setLogLevel(LogLevel.INFO);`). Only log messages of the set threshold or above will be logged. Each log messages is prefixed by the originating class by default, to turn off this behavior use `Logger.setSkipPrefix(true);`. When using [ExPlat][explat], logging messages are printed to job specific files, which are stored in a special directory created for the experiment. 

All classes that output log messages include a public static object called `LOG` of type `edu.cornell.cs.utils.log.ILogger`. All logging messages within the class are created using the logger. This object also provides more granular control of the logging level. You may set a custom log level for each class. For example, to set the log level for the multi-threaded parser to DEBUG use `edu.cornell.cs.lil.tiny.parser.ccg.cky.multi.MultiCKYParser.LOG.setCustomLevel(LogLevel.DEBUG)`. Note that this will only affect the log messages created within this class, and not any parent or child classes. 

Want to use the logging system in your own code or add new log messages? You can view the interface `edu.cornell.cs.utils.log.ILogger` to see what logging methods are available.

### ExPlat[explat]

ExPlat is SPF's experiments platform. It's intended to streamline experiments and help you avoid huge `main(String[])` methods that just initialize one things after the other and are a pain to update. 

An ExPlat experiments is defined by two parts: a backend class in the code and a .exp file. For example, consider the GeoQuery experiment accompanying SPF. The class `edu.cornell.cs.lil.tiny.geoquery.GeoExp` provides the backend code, while the .exp files in `geoquery/experiments` define the resources used and the jobs to be executed. For example, the file `geoquery/experiments/template/test/test.exp` defines the evaluation experiment. In this experiment we use the 10 development folds for training and test on the held-out set.

Each .exp file includes three sections separated by empty lines:  
1. Global parameters  
2. Resources  
3. Jobs  
Empty lines may not appear in any part of the .exp file or any included files, except to separate between the three sections. In general, each line includes a single directive. The `include=<file>` directive may be used at any point to include a file. The file is included in similar style to C include statements, meaning: the text is pasted instead of the `include` directive. The path of the included file should be relative to the location of the .exp file. Each directive (line) is a white-space separated sequence of key-value pairs (`key=value`). To refer to the value of a previously defined parameters use `%{parameter_name}` in the value slot. The reference is resolved first in the local scope of the current directive, and if no parameter of that name is found, it is resolved in the scope of the global parameters. The parameters are read in sequence and the namespace of the current directive is updated in that order. 

#### Global Parameters
This section includes various parameters that may be used when defining resources and jobs later. Each parameter is defined as a key-value directive: `key=value`. Each directive defines a single parameter. The values are read as strings. The parameters are either used in the code or by the rest of the .exp file. For example, to see which parameters are used by the GeoQuery experiment, see the parameters read by the constructor of `edu.cornell.cs.lil.tiny.geoquery.GeoExp` and the classes it extends.

#### Resources
Each directive in this section defines a resource that can be used later, either in other resources or in jobs. The `id` parameter defines the name of the resource. Resources IDs may not override previously defined names. The `type` parameter defines the type of the resource. The available resources are registered in the code. For example, the class `edu.cornell.cs.lil.tiny.geoquery.GeoResourceRepo` is responsible for registering resources for the GeoQuery experiment. To use a resource its creator is registered. A creator is basically a factory that is able to get a directive in the form of parameters, interpret them and instantiate an object. The `type()` of each creator provides the type of the resource. The other key-value pairs provide various parameters that are interpreted by the resource creator. See the creators themselves for the available parameters. All creators implement the interface `edu.cornell.cs.lil.tiny.explat.resources.IResourceObjectCreator`. The resource creators each define a usage() function which documents the available parameters of the resource.


#### Jobs
Each directive in this section defines a job to be executed. The type of the job is defined by the `type` parameter. The ID of the job is defined by the `id` parameter. The jobs on which this job depends are defined by the `dep` parameter, which includes a comma-separated list of job IDs. Jobs are executed in parallel, unless a dependency is declared. The rest of the parameters define arguments for the job. These may refer to resources, global parameters or directly include values. Job directives are interpreted by methods in the experiment class. For example, in the GeoQuery experiment the class `edu.cornell.cs.lil.tiny.geoquery.GeoExp` includes a number of methods to read job directives and create jobs, see the method `createJob(Parameters)` for the available job types and the methods that handle them.

### Working with Logical Expressions

See `LogicalLanaguageServices` for the main service class that is required for the logical language to work. Most operations on logical expressions are done using the visitor design pattern, see `ILogicalExpressionVisitor`. 

More coming soon ... 

### Combinatory Categorial Grammars (CCGs) in SPF

Coming soon ... 

#### Basic Operations on Categories

See `ICatagoeryServices`.

More coming soon ... 

### Known Issues

**The CKY parser output doesn't marginalize properly over logical forms under certain conditions.**  
The class `CKYDerivation` stores a single CKY chart cell, which means that it stores a single category (pairing of syntax and logical form). When the parser is defined to consider more than one syntactic category as a complete parse, it will often lead to different CKY parses with the same logical form. This may lead to errors in computing a logical form's probability. To correctly compute the probably, collect the inside score of all `CKYDerivation` objects with the same logical form, add their inside score and normalize. 

### Troubleshooting

**I am getting NaNs and/or infinity values in my updates or an exception saying my updates are invalid.**  
If you are using an exponentiated model with gradient updates, try to adjust the learning rate. With an exponentiated model the values might get too large and it's advised to scale them down.

**I am having trouble re-creating the results from Kwiatkowski et al. 2010 and Kwiatkowski et al. 2011 with the Unification-based GENLEX procedure.**  
The unification code in SPF is not identical to the original paper. The code for the original paper is available [online](https://homes.cs.washington.edu/~lsz/code/UBL.tgz). If you want to re-create the results, this is the way to go. This code is basically a very old version of what SPF started from. Be warned, it's messy. The code in SPF is doing a few things differently, including more liberal splitting and no support of certain features that the original code contains. We hope to bring SPF's version of splitting closer to the original paper in the future. 

**When using a factored lexicon, I sometimes see two lexical entries that have a similar structure factored into different lexical templates, although they should share the same template.**  
This is a known issue in the implementation of factoring the current version of SPF. We hope to update the framework soon with a fix, but have no concrete date. However, in practice, we noticed no degradation in performance due to this issue. 

## Publications and Projects Using SPF

Please let us know if you used SPF in your published work and we will be happy to list it here. 

Tom Kwiatkowski, Eunsol Choi, Yoav Artzi and Luke Zettlemoyer. [Scaling Semantic Parsers with On-the-fly Ontology Matching](http://yoavartzi.com/pub/kcaz-emnlp.2013.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2013.

Nicholas FitzGerald, Yoav Artzi and Luke Zettlemoyer. [Learning Distributions over Logical Forms for Referring Expression Generation](http://yoavartzi.com/pub/faz-emnlp.2013.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2013. [Code and data](http://yoavartzi.com/navi).

Yoav Artzi and Luke Zettlemoyer. [Weakly Supervised Learning of Semantic Parsers for Mapping Instructions to Actions](http://yoavartzi.com/pub/az-tacl.2013.pdf). In Transactions of the Association for Computational Linguistics (TACL), 2013.

## Acknowledgements

We would like to thank our early users for helping making SPF better (in alphabetical order):  
Sebastian Beschke, Hamburg University  
Eunsol Choi, The University of Washington  
Nicholas FitzGerald, The University of Washington  
Jun Ki Lee, Brown University 
Kenton Lee, The University of Washington  
Gabriel Schubiner, The University of Washington  
Adrienne Wang, The University of Washington  

## License

Cornell SPF - The Cornell Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 51
Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
