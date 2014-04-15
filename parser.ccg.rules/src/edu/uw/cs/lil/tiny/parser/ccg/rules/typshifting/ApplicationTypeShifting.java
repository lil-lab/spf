package edu.uw.cs.lil.tiny.parser.ccg.rules.typshifting;

import java.util.Collection;
import java.util.Collections;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IUnaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;
import edu.uw.cs.lil.tiny.parser.ccg.rules.RuleName;
import edu.uw.cs.lil.tiny.parser.ccg.rules.UnaryRuleName;
import edu.uw.cs.utils.collections.ListUtils;

/**
 * Type shifting rule that operates by applying a pre-defined category to the
 * input category.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Meaning representation.
 */
public class ApplicationTypeShifting<MR> implements IUnaryParseRule<MR> {
	
	private final ICategoryServices<MR>	categoryServices;
	private final ComplexCategory<MR>	function;
	private final Syntax				inputSyntax;
	private final UnaryRuleName			ruleName;
	
	public ApplicationTypeShifting(String label, ComplexCategory<MR> function,
			ICategoryServices<MR> categoryServices) {
		this.ruleName = UnaryRuleName.create(label);
		this.function = function;
		this.categoryServices = categoryServices;
		this.inputSyntax = function.getSyntax().getRight();
	}
	
	@Override
	public Collection<ParseRuleResult<MR>> apply(Category<MR> category) {
		final Category<MR> shifted = categoryServices.apply(function, category);
		if (shifted == null) {
			return Collections.emptyList();
		} else {
			return ListUtils.createSingletonList(new ParseRuleResult<MR>(
					ruleName, shifted));
		}
	}
	
	@Override
	public RuleName getName() {
		return ruleName;
	}
	
	@Override
	public boolean isValidArgument(Category<MR> category) {
		return category.getSyntax().equals(inputSyntax);
	}
	
	@Override
	public String toString() {
		return ruleName.toString();
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<ApplicationTypeShifting<MR>> {
		
		private final String	type;
		
		public Creator() {
			this("rule.shifting.generic.application");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public ApplicationTypeShifting<MR> create(Parameters params,
				IResourceRepository repo) {
			final ICategoryServices<MR> categoryServices = repo
					.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE);
			return new ApplicationTypeShifting<MR>(params.get("name"),
					(ComplexCategory<MR>) categoryServices.parse(params
							.get("function")), categoryServices);
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					ApplicationTypeShifting.class)
					.addParam("name", String.class, "Rule name")
					.addParam("function", ComplexCategory.class,
							"Function category.").build();
		}
		
	}
	
}
