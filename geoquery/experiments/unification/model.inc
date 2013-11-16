## Default scorers
type=scorer.uniform id=uniform0Scorer   class=lexEntry  weight=0.0
type=scorer.lexeme.cooc id=coocScorer file=../../../resources/geo600.dev.giza_probs
type=scorer.lex.skipping   id=skippingScorer cost=%{wordSkippingCost}  baseScorer=uniform0Scorer
## Features
type=feat.logexp.coordination id=featLogexpCoordination cpp1=true cpap=true rept=true
type=feat.lex init=skippingScorer id=featLex
type=feat.lexeme id=featLexeme init=coocScorer
type=feat.lextemplate id=featLextemplate scale=0.1
## Lexicon
type=lexicon.factored id=lexicon
## Actual model
type=model id=model lexicon=lexicon initialLexicon=initialLexicon lexicalFeatures=featLex,featLextemplate,featLexeme parseFeatures=featLogexpCoordination
## Debug model logger
type=logger.model id=loggerModel cluster=true
