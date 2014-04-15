package edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeraising;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ITypeRaisingRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName.Direction;
import edu.uw.cs.lil.tiny.parser.ccg.rules.UnaryRuleName;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.filter.IFilter;

public abstract class AbstractTypeRaising implements
		ITypeRaisingRule<LogicalExpression, Type> {
	private final Direction			direction;
	private final UnaryRuleName		ruleName;
	private final IFilter<Syntax>	validSyntaxFilter;
	
	public AbstractTypeRaising(Direction direction,
			IFilter<Syntax> validSyntaxFilter) {
		this.direction = direction;
		this.validSyntaxFilter = validSyntaxFilter;
		this.ruleName = TypeRaisingNameServices.createRuleName(direction);
	}
	
	@Override
	public ParseRuleResult<LogicalExpression> apply(
			Category<LogicalExpression> category, Syntax innerArgument,
			Syntax finalResult, Type finalResultSemanticType) {
		if (!innerArgument.equals(category.getSyntax())
				|| validSyntaxFilter.isValid(category.getSyntax())) {
			final LogicalExpression raisedSemantics = raiseSemantics(
					category.getSem(), finalResultSemanticType);
			
			// Create the raised category, including the syntactic component.
			if (direction.equals(Direction.FORWARD)) {
				return new ParseRuleResult<LogicalExpression>(ruleName,
						new ComplexCategory<LogicalExpression>(
								new ComplexSyntax(finalResult,
										new ComplexSyntax(finalResult,
												innerArgument, Slash.BACKWARD),
										Slash.FORWARD), raisedSemantics));
			} else if (direction.equals(Direction.BACKWARD)) {
				return new ParseRuleResult<LogicalExpression>(ruleName,
						new ComplexCategory<LogicalExpression>(
								new ComplexSyntax(finalResult,
										new ComplexSyntax(finalResult,
												innerArgument, Slash.FORWARD),
										Slash.BACKWARD), raisedSemantics));
			} else {
				throw new IllegalStateException("invalid direction");
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractTypeRaising other = (AbstractTypeRaising) obj;
		if (direction == null) {
			if (other.direction != null) {
				return false;
			}
		} else if (!direction.equals(other.direction)) {
			return false;
		}
		if (ruleName == null) {
			if (other.ruleName != null) {
				return false;
			}
		} else if (!ruleName.equals(other.ruleName)) {
			return false;
		}
		if (validSyntaxFilter == null) {
			if (other.validSyntaxFilter != null) {
				return false;
			}
		} else if (!validSyntaxFilter.equals(other.validSyntaxFilter)) {
			return false;
		}
		return true;
	}
	
	@Override
	public RuleName getName() {
		return ruleName;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((direction == null) ? 0 : direction.hashCode());
		result = prime * result
				+ ((ruleName == null) ? 0 : ruleName.hashCode());
		result = prime
				* result
				+ ((validSyntaxFilter == null) ? 0 : validSyntaxFilter
						.hashCode());
		return result;
	}
	
	@Override
	public boolean isValidArgument(Category<LogicalExpression> category) {
		return validSyntaxFilter.isValid(category.getSyntax());
	}
	
	private LogicalExpression raiseSemantics(LogicalExpression sem,
			Type finalResultSemanticType) {
		final Variable variable = new Variable(LogicLanguageServices
				.getTypeRepository().getTypeCreateIfNeeded(
						LogicLanguageServices.getTypeRepository()
								.generalizeType(finalResultSemanticType),
						LogicLanguageServices.getTypeRepository()
								.generalizeType(sem.getType())));
		return new Lambda(variable, new Literal(variable,
				ListUtils.createSingletonList(sem)));
	}
}
