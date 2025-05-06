package solversIntegratedRouting;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import instance.Instance_TT;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.Event;
import pesp.ModelArcFormulation;

public class SolverArcReduced extends Solver_TT {
	private Instance_TT reducedInst;
	private Solution_TT reducedSol;
	
	public SolverArcReduced(String name, int cpuTime, boolean smartRouting) throws IOException {
		super(name, cpuTime,smartRouting);
		if(!name.equals("Stuttgart")) {
			inst.completeActivities();
		}
		reducedInst = inst.getMPPESPfromPESP();
		
	}

	public void solve(int transferPercentage) throws IloException {
		reducedInst.computeRouting(false);
		reducedInst.printStats();
		reducedInst.ignoreFreeActivities(transferPercentage);
		EAN red = new EAN(reducedInst);
		red.determineSubgraphPerPeriod();
		ModelArcFormulation arc_red = new ModelArcFormulation(red, 0);
		arc_red.solve(maxCPU);
		currentSol = new Solution_TT(reducedInst,arc_red.getTensions(),arc_red.getPotentials(),true);
	}
	
	public void solveByComponent(int transferPercentage) throws IloException {
		reducedInst.printStats();
		reducedInst.ignoreFreeActivities(transferPercentage);
		EAN red = new EAN(reducedInst);
		List<EAN> components = red.getConnectedComponentsEAN();
		Map<Activity, Integer> tensions = new HashMap<>();
		Map<Event, Integer> potentials = new HashMap<>();
		int compCount = 1;
		for(EAN comp: components) {
			comp.determineSubgraphPerPeriod();
			
			ModelArcFormulation arc_comp = new ModelArcFormulation(comp,0);
			if(reducedSol!=null) {
				arc_comp.setStartSolution(reducedSol);
			}
			//we now use the same cpu for each component
			System.out.println("Start solving component "+compCount + " out of "+components.size());
			arc_comp.solve(maxCPU);
			tensions.putAll(arc_comp.getTensions());
			potentials.putAll(arc_comp.getPotentials());
			compCount++;
		}
		
		reducedSol = new Solution_TT(reducedInst,tensions,potentials,true);
		System.out.println("Reduced sol has travel time: "+reducedSol.getTravelTime());
		currentSol = new Solution_TT(inst,reducedSol);
		System.out.println("Final sol has travel time: "+currentSol.getTravelTime());
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

