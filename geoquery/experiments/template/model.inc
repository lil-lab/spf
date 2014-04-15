## Default scorers
type=scorer.uniform id=uniform0Scorer   class=lexEntry  weight=0.0
type=scorer.lex.skipping   id=skippingScorer cost=%{wordSkippingCost}  baseScorer=uniform0Scorer
## Features
type=feat.logexp.coordination id=featLogexpCoordination cpp1=true cpap=true rept=true
type=feat.lex init=skippingScorer id=featLex
type=feat.lexeme id=featLexeme 
type=feat.lextemplate id=featLextemplate scale=0.1
type=feat.rules.count id=featRules scale=0.1
## Lexicon
type=lexicon.factored id=lexicon
## Actual model
type=model id=model lexicon=lexicon lexicalFeatures=featLex,featLextemplate,featLexeme parseFeatures=featLogexpCoordination,featRules
## Debug model logger
type=logger.model id=loggerModel cluster=true
