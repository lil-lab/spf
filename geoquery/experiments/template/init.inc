## Model initializers
type=init.lex   id=initSeedLex lexicon=seedLexicon
type=init.lex	id=initNPs	lexicon=npLexicon
type=scorer.lenexp id=expScorer coef=10.0 exp=1.1
type=init.lex.weights id=initSeedLexWeights lexicon=seedLexicon key=FACLEX#LEX scorer=expScorer
type=init.lex.weights id=initSeedXemeWeights lexicon=seedLexicon key=FACLEX#XEME value=10.0
type=init.lex.weights id=initNPsLexWeights lexicon=npLexicon key=FACLEX#LEX scorer=expScorer
type=init.lex.weights id=initNPsXemeWeights lexicon=npLexicon key=FACLEX#XEME value=10.0
type=init.weights id=initWeights file=../../../resources/init.weights
