package solversMultiPeriodPESP;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloException;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.ModelArcFormulation;
import pesp.ModelCycleFormulation;

public class SolverEPESPArcReduced extends Solver_EPESP {

	public SolverEPESPArcReduced(String name, int cpuTime) throws IOException {
		super(name, cpuTime);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void solve() throws IloException {
		//inst.ignoreFreeActivities();
		EAN red = new EAN(inst);
		nEvents = red.getNumberOfNodes();
		nArcs = red.getNumberOfArcs();
		
		ModelArcFormulation arc_red = new ModelArcFormulation(red,0);
		if(startSolution!=null) {
			arc_red.setStartSolution(startSolution);
		}
		
		double startTime = System.nanoTime();
		arc_red.solve(maxCPU);
		cpuTime = 1e-9*(System.nanoTime()-startTime);

		status = arc_red.getStatus();
		objective = Double.MAX_VALUE;
		if(status.equals("optimal")||status.equals("feasible")) {
			objective = arc_red.getObjective();
			sol = new Solution_TT(inst,arc_red.getTensions(),arc_red.getPotentials(),false);
		}
		gap = arc_red.getGap();
		System.out.println("Solved with objective: "+arc_red.getObjective());
		
		
//		Map<Activity,Integer> tensions = new HashMap<>();
//		tensions.putAll(cycle_red.getTensions());
//		sol = new Solution_TT(inst,red,tensions,true);
		
	}

}
