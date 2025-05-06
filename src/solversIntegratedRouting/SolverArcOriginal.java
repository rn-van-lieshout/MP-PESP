package solversIntegratedRouting;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.Event;
import pesp.ModelArcFormulation;

public class SolverArcOriginal extends Solver_TT {

	
	public SolverArcOriginal(String name, int cpuTime, boolean smartRouting) throws IOException {
		super(name, cpuTime, smartRouting);
	}

	public void solve(int transferPercentage) throws IloException {
		inst.computeRouting(false);
		inst.printStats();
		inst.ignoreFreeActivities(transferPercentage);
		EAN ori = new EAN(inst);
		ori.determineSubgraphPerPeriod();
		ModelArcFormulation arc_ori = new ModelArcFormulation(ori,0);
		arc_ori.solve(maxCPU);
		currentSol = new Solution_TT(inst,arc_ori.getTensions(),arc_ori.getPotentials(),true);
	}

	public void solveByComponent(int transferPercentage) throws IloException {
		inst.printStats();
		inst.ignoreFreeActivities(transferPercentage);
		EAN ori = new EAN(inst);
		List<EAN> components = ori.getConnectedComponentsEAN();
		Map<Activity, Integer> tensions = new HashMap<>();
		Map<Event, Integer> potentials = new HashMap<>();
		int compCount = 1;
		for(EAN comp: components) {
			comp.determineSubgraphPerPeriod();
			
			ModelArcFormulation arc_comp = new ModelArcFormulation(comp,0);
			if(bestSol!=null) {
				arc_comp.setStartSolution(bestSol);
			}
			//we now use the same cpu for each component
			System.out.println("Start solving component "+compCount + " out of "+components.size());
			arc_comp.solve(maxCPU);
			tensions.putAll(arc_comp.getTensions());
			potentials.putAll(arc_comp.getPotentials());
			compCount++;
		}
		
		currentSol = new Solution_TT(inst,tensions,potentials,true);
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
