package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Represents the stage of a pipeline. It will contain information about the stage, e.g., what needs
 * to come in, what needs to go out and what does it mod/ref, interprocedurally.
 * 
 * @author nchen
 * 
 */
public class PipelineStage {
	private ProgramDependenceGraph pdg;

	private List<Integer> selectedLines; // This is 1-based following how things "look" in the editor

	private List<PDGNode> selectedStatements= new ArrayList<PDGNode>(); // The actual selected statements (PDGNodes) in the editor

	// These are data dependencies that have to be explicitly tracked and passed/received during the transformation, i.e., they are expressable in code

	private List<DataDependence> inputDataDependences; // Think of these as method parameters

	private List<DataDependence> outputDataDependences; // Think of these as method return value(S) <-- yes possibly values!

	private Set<String> closureLocalVariableNames;

	// These are data dependencies that have to be implicitly tracked for feasibility but they are not visible in source code

	private Set<PointerKey> refs= new HashSet<PointerKey>();

	private Set<PointerKey> mods= new HashSet<PointerKey>();

	/*
	 * Convenience method to create a new pipeline stage and begin the analysis immediately
	 */
	public static PipelineStage makePipelineStage(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		PipelineStage temp= new PipelineStage(pdg, selectedLines);
		temp.analyzeSelection();
		return temp;
	}

	public PipelineStage(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		this.pdg= pdg;
		this.selectedLines= selectedLines;
	}

	public void analyzeSelection() {
		computeSelectedStatements();
		inputDataDependences= computeInput();
		outputDataDependences= computeOutput();
		closureLocalVariableNames= filterMethodParameters(computeLocalVariables());
	}

	private void computeSelectedStatements() {
		for (PDGNode node : Iterator2Iterable.make(pdg.iterator())) {
			for (Integer line : selectedLines) {
				if (node.isOnLine(line))
					selectedStatements.add(node);
			}
		}
	}

	// Get all successor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeOutput() {
		List<DataDependence> outputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode succ : Iterator2Iterable.make(pdg.getSuccNodes(node))) {
				if (notPartOfInternalNodes(succ))
					outputs.addAll(pdg.getEdgeLabels(node, succ));
			}
		}

		return outputs;
	}

	// Get all the predecessor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeInput() {
		List<DataDependence> inputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode pred : Iterator2Iterable.make(pdg.getPredNodes(node))) {
				if (notPartOfInternalNodes(pred))
					inputs.addAll(pdg.getEdgeLabels(pred, node));
			}
		}
		return inputs;
	}

	private boolean notPartOfInternalNodes(PDGNode node) {
		for (PDGNode internalNode : selectedStatements) {
			if (internalNode == node)
				return false;
		}
		return true;
	}

	private Set<String> computeLocalVariables() {
		Set<String> localVariables= new HashSet<String>();
		for (PDGNode node : selectedStatements) {
			localVariables.addAll(node.defs());
		}
		return localVariables;
	}

	private Set<String> filterMethodParameters(Set<String> localVariables) {
		Set<String> parameterNames= new HashSet<String>();
		for (DataDependence data : inputDataDependences) {
			parameterNames.addAll(data.getLocalVariableNames());
		}
		localVariables.removeAll(parameterNames); // Anything that is passed in as a parameter should not be redeclared
		return localVariables;
	}

	public List<DataDependence> getInputDataDependences() {
		return inputDataDependences;
	}

	public List<DataDependence> getOutputDataDependences() {
		return outputDataDependences;
	}

	public Set<String> getClosureLocalVariableNames() {
		return closureLocalVariableNames;
	}

	public List<PDGNode> getSelectedStatements() {
		return selectedStatements;
	}

	private List<SSAInstruction> retrieveAllSSAInstructions() {
		List<SSAInstruction> ssaInstructions= new ArrayList<SSAInstruction>();

		for (PDGNode node : selectedStatements) {
			if (node instanceof Statement) {
				Statement statement= (Statement)node;
				ssaInstructions.addAll(statement.retrieveAllSSAInstructions());
			}
		}

		return ssaInstructions;
	}

	// Modref analysis
	// Though this might look more complicated, we intentionally split this up (not doing modref upfront).
	// This facilitates a staged approach to determining feasibility of each pipeline stage and also makes
	// it easier to test in isolation.
	void computeHeapDependencies(CGNode cgNode, PointerAnalysis pointerAnalysis, ModRef modref) {
		PipelineStageModRef pipelineStageModRef= new PipelineStageModRef(cgNode, pointerAnalysis, modref);
		pipelineStageModRef.computeHeapDependencies();
	}

	class PipelineStageModRef {
		private CGNode cgNode;

		private PointerAnalysis pointerAnalysis;

		private ModRef modref;

		private DelegatingExtendedHeapModel heapModel;

		private HeapExclusions exclusions; //TODO: Might want to make use of this to filter out JDK classes

		public PipelineStageModRef(CGNode cgNode, PointerAnalysis pointerAnalysis, ModRef modref) {
			this.cgNode= cgNode;
			this.pointerAnalysis= pointerAnalysis;
			this.modref= modref;
			this.heapModel= new DelegatingExtendedHeapModel(pointerAnalysis.getHeapModel());
		}

		void computeHeapDependencies() {
			computeRefs();
			computeMods();
		}

		private void computeMods() {
			List<SSAInstruction> instructions= retrieveAllSSAInstructions();
			for (SSAInstruction instruction : instructions) {
				mods.addAll(modref.getMod(cgNode, heapModel, pointerAnalysis, instruction, null));
			}
		}

		private void computeRefs() {
			List<SSAInstruction> instructions= retrieveAllSSAInstructions();
			for (SSAInstruction instruction : instructions) {
				refs.addAll(modref.getRef(cgNode, heapModel, pointerAnalysis, instruction, null));
			}
		}
	}

	public Set<PointerKey> getRefs() {
		return refs;
	}

	public Set<PointerKey> getMods() {
		return mods;
	}
}