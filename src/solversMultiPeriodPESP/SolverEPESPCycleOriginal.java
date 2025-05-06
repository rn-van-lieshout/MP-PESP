package solversMultiPeriodPESP;

import java.io.IOException;
import ilog.concert.IloException;
import instance.Instance_TT;
import instance.Solution_TT;
import pesp.EAN;
import pesp.ModelCycleFormulation;

public class SolverEPESPCycleOriginal extends Solver_EPESP {

	public SolverEPESPCycleOriginal(String name, int cpuTime) throws IOException {
		super(name, cpuTime);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void solve() throws IloException {
		//inst.ignoreFreeActivities();
		Instance_TT full = inst.getPESPfromMPPESP();
		
		EAN ean = new EAN(full);
		ean.determineSubgraphPerPeriod();
		nEvents = ean.getNumberOfNodes();
		nArcs = ean.getNumberOfArcs();
		
		ModelCycleFormulation cycle_ori = new ModelCycleFormulation(ean,false,full.getConstantTerm());
		double startTime = System.nanoTime();
		cycle_ori.solve(maxCPU);
		cpuTime = 1e-9*(System.nanoTime()-startTime);

		status = cycle_ori.getStatus();
		objective = Double.MAX_VALUE;
		if(status.equals("optimal")||status.equals("feasible")) {
			objective = cycle_ori.getObjective();
			sol = new Solution_TT(full,ean,cycle_ori.getTensions(),cycle_ori.getMST(),false);
		}
		gap = cycle_ori.getGap();
		System.out.println("Solved with objective: "+cycle_ori.getObjective());
		
	}

}
