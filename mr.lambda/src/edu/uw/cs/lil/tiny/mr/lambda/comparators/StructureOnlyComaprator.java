package edu.uw.cs.lil.tiny.mr.lambda.comparators;

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.mr.lambda.ILogicalExpressionComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetStructure;

/**
 * Compare the structure of the logical form. Ignores constant names (but not
 * types).
 * 
 * @author Yoav Artzi
 */
public class StructureOnlyComaprator implements ILogicalExpressionComparator {
	
	private final ILogicalExpressionComparator	baseComparator;
	
	public StructureOnlyComaprator(ILogicalExpressionComparator baseComparator) {
		this.baseComparator = baseComparator;
	}
	
	@Override
	public boolean compare(LogicalExpression o1, LogicalExpression o2) {
		final LogicalExpression anonO1 = GetStructure.of(o1);
		final LogicalExpression anonO2 = GetStructure.of(o2);
		return baseComparator.compare(anonO1, anonO2);
	}
	
	public static class Creator implements
			IResourceObjectCreator<StructureOnlyComaprator> {
		
		private final String	type;
		
		public Creator() {
			this("comparator.structonly");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public StructureOnlyComaprator create(Parameters params,
				IResourceRepository repo) {
			return new StructureOnlyComaprator(
					params.contains("comparator") ? (ILogicalExpressionComparator) repo
							.getResource("comparator") : LogicLanguageServices
							.getComparator());
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					StructureOnlyComaprator.class).addParam("comparator",
					ILogicalExpressionComparator.class,
					"Base comparator to use (default is taken from LLS)")
					.build();
		}
		
	}
	
}
