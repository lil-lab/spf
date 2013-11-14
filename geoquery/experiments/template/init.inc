## Model initializers
type=init.lex   id=initSeedLex lexicon=seedLexicon
type=init.lex	id=initNPs	lexicon=npLexicon
type=scorer.lenexp id=expScorer coef=10.0 exp=1.1
type=init.lex.weights id=initSeedLexWeights lexicon=seedLexicon tag=LEX scorer=expScorer
type=init.lex.weights id=initSeedXemeWeights lexicon=seedLexicon tag=XEME value=10.0
type=init.lex.weights id=initNPsLexWeights lexicon=npLexicon tag=LEX scorer=expScorer
type=init.lex.weights id=initNPsXemeWeights lexicon=npLexicon tag=XEME value=10.0
