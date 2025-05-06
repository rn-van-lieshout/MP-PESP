package solversMultiPeriodPESP;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloException;
import instance.Solution_TT;
import pesp.Activity;
import pesp.EAN;
import pesp.ModelCycleFormulation;

public class SolverEPESPCycleReduced extends Solver_EPESP {

	public SolverEPESPCycleReduced(String name, int cpuTime) throws IOException {
		super(name, cpuTime);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void solve() throws IloException {
		EAN red = new EAN(inst);
//		red.determineEanPerPeriod();
//		if(!red.isNice()) {
//			red.makeNice();
//		}
		red.makeRootedPerComponent();
		nEvents = red.getNumberOfNodes();
		nArcs = red.getNumberOfArcs();
		
		ModelCycleFormulation cycle_red = new ModelCycleFormulation(red,true,0);
		if(startSolution!=null) {
			cycle_red.setInitialSolution(startSolution);
		}
		
		double startTime = System.nanoTime();
		cycle_red.solve(maxCPU);
		cpuTime = 1e-9*(System.nanoTime()-startTime);

		status = cycle_red.getStatus();
		objective = Double.MAX_VALUE;
		if(status.equals("optimal")||status.equals("feasible")) {
			objective = cycle_red.getObjective();
			sol = new Solution_TT(inst, red, cycle_red.getTensions(), cycle_red.getMST(),false);
		}
		gap = cycle_red.getGap();
		System.out.println("Solved with objective: "+cycle_red.getObjective());
	}

}
