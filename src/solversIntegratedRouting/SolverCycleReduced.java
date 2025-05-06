package solversIntegratedRouting;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digraph.DirectedGraph;
import digraph.DirectedGraphArc;
import ilog.concert.IloException;
import instance.Activity_TT;
import instance.Instance_TT;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.Event;
import pesp.ModelArcFormulation;
import pesp.ModelCycleFormulation;

public class SolverCycleReduced extends Solver_TT {
	private Instance_TT reducedInst;
	private Solution_TT reducedSol;

	public SolverCycleReduced(String name, int cpuTime, boolean smartRouting) throws IOException {
		super(name, cpuTime, smartRouting);
		if(!name.equals("Stuttgart")) {
			inst.completeActivities();
		}
		//System.out.println("NOT COMPLETING ACTIVITIES");
		reducedInst = inst.getMPPESPfromPESP();
	}

	@Override
	public void solve(int transferPercentage) throws IloException {
		reducedInst.computeRouting(true);
		reducedInst.printStats();
		reducedInst.ignoreFreeActivities(transferPercentage);
		EAN red = new EAN(reducedInst);
		Map<Activity,Integer> tensions = new HashMap<>();
		red.determineSubgraphPerPeriod();
		if(!red.isRooted()) {
			red.makeRooted();
		}
		ModelCycleFormulation cycle_red = new ModelCycleFormulation(red,true,0);
		cycle_red.solve(maxCPU);
		tensions.putAll(cycle_red.getTensions());
		reducedSol = new Solution_TT(reducedInst,red,tensions,cycle_red.getMST(),true);
		currentSol = new Solution_TT(inst,reducedSol);
		bestSol = currentSol;
	}

	@Override
	public void solveByComponent(int transferPercentage) throws IloException, FileNotFoundException {
		reducedInst.printStats();
		double totObjective = 0;
		reducedInst.ignoreFreeActivities(transferPercentage);
		EAN red = new EAN(reducedInst);
		Map<Activity,Integer> tensions = new HashMap<>();
		int compCount = 1;
		List<EAN> components = red.getConnectedComponentsEAN();
		totObjective = 0;
		DirectedGraph<Event,Activity> minimumSpanningForest = new DirectedGraph<>();
		for(EAN component: components) {
			component.determineConnectedComponentsEAN();
			System.out.println("\n Start solving component "+compCount + " out of "+components.size());
			
			component.determineSubgraphPerPeriod();
			if(!component.isRooted()) {
				component.makeRooted();
			}
			
			if(component.getNumberOfArcs()<5) {
				System.out.println("Fewer than 5 arcs");
				for(DirectedGraphArc<Event, Activity> a: component.getArcs()) {
					System.out.println("in component: "+a.getData());
				}
			}
			//TODO: ADD SPANNING TREE IN COMPONENT TO MINIMUM SPANNING FOREST
			ModelCycleFormulation cycle_red = new ModelCycleFormulation(component,true,0);
			if(reducedSol!=null) {
				cycle_red.setInitialSolution(reducedSol);
			}
			cycle_red.solve(maxCPU);
			minimumSpanningForest.addGraph(cycle_red.getMST());
			tensions.putAll(cycle_red.getTensions());
			totObjective += cycle_red.getObjective();
			compCount++;
		}
		System.out.println("\n Finished solving all components, now putting solution together");
		totObjective += reducedInst.getTotTransferPenalty();
		System.out.println("Total objective: "+totObjective);
		//see todo!!!!!
		reducedSol = new Solution_TT(reducedInst,red,tensions,minimumSpanningForest,true);
		currentSol = new Solution_TT(inst,reducedSol);
		System.out.println("Reduced sol has travel time: "+reducedSol.getTravelTime());
		System.out.println("New sol has travel time: "+currentSol.getTravelTime());
	}
	
	@Override
	public void computeRouting() {
		reducedInst.computeRouting(smartRouting);
	}

	@Override
	public void recomputeWeights() {
		reducedInst.recomputeWeights(reducedSol.getRoutingNetwork());
	}

}
