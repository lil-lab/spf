# [_**UW SPF v1.2**_](http://yoavartzi.com/spf) - The University of Washington Semantic Parsing Framework v1.2

**Developed and maintained by** [Yoav Artzi](http://yoavartzi.com)

**Contributors:** [Luke Zettlemoyer](http://homes.cs.washington.edu/~lsz/), [Tom Kwiatkowski](http://homes.cs.washington.edu/~tomk/)

## Documentations

More coming soon … 

# Building

To compile SPF use: `ant dist`. The output JAR file will be in the `dist` directory. You can also download the compiled JAR file from the [Downloads](https://bitbucket.org/yoavartzi/spf/downloads) section.

## Attribution

When using UW SPF, please acknowledge it by citing:

Artzi, Yoav and Zettlemoyer, Luke. "UW SPF: The University of Washington Semantic Parsing Framework." http://yoavartzi.com/spf.  2013.

**Bibtex:**

    @article{artzi2013uwspf,
        title={{UW SPF: The University of Washington Semantic Parsing Framework}},
        author={Artzi, Yoav and Zettlemoyer, Luke},
        year={2013}
    }

When using specific algorithms please cite the appropriate work:

### Validation based learning, joint inference and coarse-to-fine lexical generation
(Classes: JointValidationSensitivePerceptron, JointModel, IJointFeatureSet, JointTemplatedAbstractLexiconGenerator, TemplatedAbstractLexiconGenerator)

Yoav Artzi and Luke Zettlemoyer. [Weakly Supervised Learning of Semantic Parsers for Mapping Instructions to Actions](http://yoavartzi.com/pub/az-tacl.2013.pdf). In Transactions of the Association for Computational Linguistics (TACL), 2013.

### Loss sensitive learning
(Classes: LossSensitivePerceptronCKY)

Yoav Artzi and Luke Zettlemoyer. [Bootstrapping Semantic Parsers from Conversations](http://yoavartzi.com/pub/2011.emnlp.az.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2011.

### Unification-based learning
(Classes: UBLStocGradient, UBLPerceptron)

Tom Kwiatkowski, Luke Zettlemoyer, Sharon Goldwater, and Mark Steedman. [Inducing Probabilistic CCG Grammars from Logical Form with Higher-order Unification](http://homes.cs.washington.edu/~lsz/papers/kzgs-emnlp2010.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2010.

### Factored lexicons
(Classes: FactoredLexicon, Lexeme, LexicalTemplate)

Tom Kwiatkowski, Luke Zettlemoyer, Sharon Goldwater, and Mark Steedman. [Lexical Generalization in CCG Grammar Induction for Semantic Parsing](http://homes.cs.washington.edu/~lsz/papers/kzgs-emnlp2011.pdf). In Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP), 2011.

### Template-driven lexical induction
(Classes: FactoredGENLEXPerceptron)

Luke Zettlemoyer and Michael Collins. [Online Learning of Relaxed CCG Grammars for Parsing to Logical Form](http://homes.cs.washington.edu/~lsz/papers/zc-emnlp07.pdf). In Proceedings of the Joint Conference on Empirical Methods in Natural Language Processing and Computational Natural Language Learning (EMNLP-CoNLL), 2007.

Luke Zettlemoyer and Michael Collins. [Learning to Map Sentences to Logical Form: Structured Classification with Probabilistic Categorial Grammars](http://homes.cs.washington.edu/~lsz/papers/zc-uai05.pdf). In Proceedings of the Twenty First Conference on Uncertainty in Artificial Intelligence (UAI), 2005.

## License

UW SPF - The University of Washington Semantic Parsing Framework. Copyright (C) 2013 Yoav Artzi

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
