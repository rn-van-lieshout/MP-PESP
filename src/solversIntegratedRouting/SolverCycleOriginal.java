package solversIntegratedRouting;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digraph.DirectedGraph;
import ilog.concert.IloException;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.Event;
import pesp.ModelArcFormulation;
import pesp.ModelCycleFormulation;

public class SolverCycleOriginal extends Solver_TT {

	public SolverCycleOriginal(String name, int cpuTime, boolean smartRouting) throws IOException {
		super(name, cpuTime, smartRouting);
		inst.completeActivities();
	}

	@Override
	public void solve(int transferPercentage) throws IloException {
		inst.computeRouting(false);
		inst.printStats();
		inst.ignoreFreeActivities(transferPercentage);
		EAN ori = new EAN(inst);
		ori.determineSubgraphPerPeriod();
		ModelCycleFormulation cycle_ori = new ModelCycleFormulation(ori,false,0);
		cycle_ori.solve(maxCPU);
		currentSol = new Solution_TT(inst,ori,cycle_ori.getTensions(),cycle_ori.getMST(),true);
		bestSol = currentSol;
	}
	
	public void solveByComponent(int transferPercentage) throws IloException {
		inst.printStats();
		inst.ignoreFreeActivities(transferPercentage);
		EAN ori = new EAN(inst);
		ori.determineSubgraphPerPeriod();
		
		List<EAN> components = ori.getConnectedComponentsEAN();
		Map<Activity, Integer> tensions = new HashMap<>();
		int compCount = 1;
		DirectedGraph<Event,Activity> minimumSpanningForest = new DirectedGraph<>();
		for(EAN comp: components) {
			comp.determineSubgraphPerPeriod();
			
			ModelCycleFormulation cycle_comp = new ModelCycleFormulation(comp,false,0);
			if(bestSol!=null) {
				cycle_comp.setInitialSolution(bestSol);
			}
			//we now use the same cpu for each component
			System.out.println("Start solving component "+compCount + " out of "+components.size());
			cycle_comp.solve(maxCPU);
			tensions.putAll(cycle_comp.getTensions());
			minimumSpanningForest.addGraph(cycle_comp.getMST());
			compCount++;
		}
		
		currentSol = new Solution_TT(inst,ori,tensions,minimumSpanningForest,true);
	}

	@Override
	public void computeRouting() {
		inst.computeRouting(smartRouting);
	}

	@Override
	public void recomputeWeights() {
		inst.recomputeWeights(currentSol.getRoutingNetwork());
	}

}
