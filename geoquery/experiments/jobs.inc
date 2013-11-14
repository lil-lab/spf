## Jobs
# Initialize
type=init model=model init=initSeedLex,initNPs,initSeedLexWeights,initNPsLexWeights,initSeedXemeWeights,initNPsXemeWeights id=init
# Log initial model
type=log id=log.pre model=model logger=loggerModel dep=init
# Learning
type=train learner=stocgrad id=train dep=log.pre model=model
# Log and test final model
type=log id=log.post model=model logger=loggerModel dep=train
type=test id=test tester=tester model=model dep=train
