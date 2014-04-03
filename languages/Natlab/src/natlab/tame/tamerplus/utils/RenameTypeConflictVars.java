package natlab.tame.tamerplus.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import natlab.tame.callgraph.SimpleFunctionCollection;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.DUChain;
import natlab.tame.tamerplus.analysis.UDChain;
import natlab.tame.tamerplus.analysis.UDDUWeb;
import natlab.tame.tamerplus.transformation.TransformationEngine;
import natlab.tame.tir.TIRAssignLiteralStmt;
import natlab.tame.tir.TIRCopyStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValueFactory;
import natlab.tame.valueanalysis.value.Args;
import natlab.tame.valueanalysis.value.ValueFactory;
import natlab.utils.AstFunctions;
import ast.ASTNode;
import ast.Expr;
import ast.Name;
import ast.NameExpr;

import com.google.common.collect.FluentIterable;

public class RenameTypeConflictVars extends TIRAbstractNodeCaseHandler {

	private static Integer suffix = 0;
	private static boolean debug = true;

	public static SimpleFunctionCollection renameConflictVarsInDifferentWebs(
			SimpleFunctionCollection callgraph,
			List<AggrValue<BasicMatrixValue>> inputValues) {
		// create an analysis
		// use this analysis and webs generated by this analysis to rename vars
		// in callgraph
		// return this callgraph

		ValueFactory<AggrValue<BasicMatrixValue>> factory = new BasicMatrixValueFactory();
		ValueAnalysis<AggrValue<BasicMatrixValue>> analysis = new ValueAnalysis<AggrValue<BasicMatrixValue>>(
				callgraph, Args.newInstance(inputValues), factory);
		
//		if (debug)
//			analysis = IntOkAnalysis.analyzeForIntOk(callgraph, inputValues);
		
		
		System.out
				.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"
						+ analysis.toString());
		System.out.println(callgraph.getPrettyPrinted());
		System.out
				.println("########################################################################################");

		// Map(table) that stores every variable name as a key to it's web and
		// type
		HashMap<String, VariableMetadata> varWebTable;
		int size = analysis.getNodeList().size();

		for (int i = 0; i < size; i++) {

			varWebTable = new HashMap<String, VariableMetadata>();
			StaticFunction function = analysis.getNodeList().get(i)
					.getFunction();
			TransformationEngine transformationEngine = TransformationEngine
					.forAST(function.getAst());
			AnalysisEngine analysisEngine = transformationEngine
					.getAnalysisEngine();

			UDDUWeb web = analysisEngine.getUDDUWebAnalysis();
			DUChain vDUChain = web.getDUChain();
			UDChain vUDChain = web.getUDChain();
			Map<String, HashSet<TIRNode>> varUses;
			Map<String, Set<TIRNode>> varDefs;
			HashSet<String> varSet = new HashSet<String>();
			VariableMetadata newDefData;
			VariableMetadata oldDefData;
			String newName;
			// get all statements
			LinkedList<TIRNode> allStatements = web.getVisitedStmtsLinkedList();
			// loop through the list, check if it is a definition in vDUChain
			// loops through only those variable definitions that are used
			// somewhere and are not return variables
			for (TIRNode statement : allStatements) {
				System.err.println("~~"+statement.toString());

				varUses = vDUChain.getUsesMapForDefinitionStmt(statement);

				if (null != varUses) {
					for (String var : varUses.keySet()) {
					  Set<String> outParamSet = FluentIterable
					      .from(function.getAst().getOutputParams())
					      .transform(AstFunctions.nameToID())
					      .toSet();
						if (!outParamSet.contains(var)) {
										// Do not rename return variable
							System.out.println("==" + statement.toString()
									+ " defines " + var + "==");
							if (!varWebTable.containsKey(var)) {
								varWebTable.put(
										var,
										getVariableMetadata(analysis,
												statement, i, var, web));
							} else {// check for renaming
									// variable has an entry in the table
									// check if this new definition has difft
									// web && (difft type or iscomplex)
									// if yes, rename it
								newDefData = getVariableMetadata(analysis,
										statement, i, var, web);
								oldDefData = varWebTable.get(var);
								if (oldDefData.getGraphNumber() != newDefData
										.getGraphNumber()
										&& (oldDefData.getMclass() != newDefData
												.getMclass() || oldDefData
												.getIsComplex() != newDefData
												.getIsComplex())) {
									newName = renameVar(analysis, statement, i,
											var, varUses);
									varWebTable.put(newName, newDefData);

								}

							}
						}
					}
				}
			}

			
		}
		return callgraph;
	}

	private static String newNameGenerator(String var) {
		suffix += 1;
		return (var + suffix.toString());
	}

	private static String renameVar(
			ValueAnalysis<AggrValue<BasicMatrixValue>> analysis,
			TIRNode statement, int i, String var,
			Map<String, HashSet<TIRNode>> varUses) {
		String newName = newNameGenerator(var);
		renameInDef(statement, var, newName);
		for (TIRNode useStmt : varUses.get(var)) {
			renameInUse(useStmt, var, newName);
		}
		return newName;

	}

	private static void renameInUse(TIRNode statement, String var,
			String newName) {

		if (statement instanceof TIRCopyStmt) {
			rename(((TIRCopyStmt) statement).getRHS(), var,
					newName);
		}

	}

	private static void renameInDef(TIRNode statement, String var,
			String newName) {

		if (statement instanceof TIRAssignLiteralStmt) {
			rename(((TIRAssignLiteralStmt) statement).getLHS(),
					var, newName);
		}

	}

	private static void rename(Expr statement, String var, String newName) {
		if (statement instanceof NameExpr) {
			((NameExpr) statement).setName(new Name(newName));
		}
		
	}

	public static VariableMetadata getVariableMetadata(
			ValueAnalysis<AggrValue<BasicMatrixValue>> analysis,
			TIRNode statement, int graphIndex, String var, UDDUWeb web) {

		if (analysis.getNodeList().get(graphIndex).getAnalysis()
				.getOutFlowSets().get(statement).isViable()) {

			BasicMatrixValue temp = ((BasicMatrixValue) (analysis
					.getNodeList().get(graphIndex).getAnalysis()
					.getOutFlowSets().get(statement).get(var).getSingleton()));
			return new VariableMetadata(web.getNodeAndColorForDefinition(var)
					.get(statement), temp.getMatlabClass().getName(), temp
					.getisComplexInfo().toString());

		}

		return null;
	}

	@Override
	public void caseASTNode(ASTNode node) {
		// TODO Auto-generated method stub

	}

}
