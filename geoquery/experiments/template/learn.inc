## Tester
type=tester id=tester data=test parser=parser
## Filter to skip long sentences during learning
type=filter.sentence.length id=learningFilter length=%{trainingMaxSentenceLength}
## Learners
type=learner.validation.perceptron id=perceptron data=train genlexbeam=%{lexbeam} iter=%{gIter} validator=validator hard=true filter=learningFilter tester=tester genlex=genlex conflateParses=true errorDriven=true
type=learner.validation.stocgrad id=stocgrad data=train genlexbeam=%{lexbeam} iter=%{gIter} validator=validator filter=learningFilter tester=tester genlex=genlex conflateParses=false errorDriven=true 
