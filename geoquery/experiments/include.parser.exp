# Parsing rules
type=rule.composition id=ruleComp
type=rule.application id=ruleApp
type=rule.shifting.pp id=shiftPP
type=rule.set.overload id=overloadedRules rules=ruleComp,ruleApp functions=shiftPP
type=rule.skipping id=ruleSkipping
type=rule.typeraise.composition.forward id=ruleRaiseComp
type=rule.thatless id=ruleThatless
type=rule.shift.pluralexists id=rulePluralExists
# Parse filter
type=parsefilter.lambda.simple id=parseFilter syntax=S
# CKY Parser
type=parser.cky.multi id=parser parseFilter=parseFilter beam=%{gBeam} preChartPruning=true pruneLexicalCells=true rules=overloadedRules,ruleSkipping,ruleRaiseComp,ruleThatless,rulePluralExists executor=executor
