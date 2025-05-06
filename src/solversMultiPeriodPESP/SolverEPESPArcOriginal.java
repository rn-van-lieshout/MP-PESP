package solversMultiPeriodPESP;

import java.io.IOException;
import ilog.concert.IloException;
import instance.Instance_TT;
import instance.Solution_TT;
import pesp.EAN;
import pesp.ModelArcFormulation;
import pesp.ModelCycleFormulation;

public class SolverEPESPArcOriginal extends Solver_EPESP {

	public SolverEPESPArcOriginal(String name, int cpuTime) throws IOException {
		super(name, cpuTime);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void solve() throws IloException {
		//inst.ignoreFreeActivities();
		Instance_TT full = inst.getPESPfromMPPESP();
		
		EAN ean = new EAN(full);
		nEvents = ean.getNumberOfNodes();
		nArcs = ean.getNumberOfArcs();
		
		ModelArcFormulation arc_ori = new ModelArcFormulation(ean,full.getConstantTerm());
		double startTime = System.nanoTime();
		arc_ori.solve(maxCPU);
		cpuTime = 1e-9*(System.nanoTime()-startTime);

		status = arc_ori.getStatus();
		objective = Double.MAX_VALUE;
		if(status.equals("optimal")||status.equals("feasible")) {
			objective = arc_ori.getObjective();
			sol = new Solution_TT(full,arc_ori.getTensions(),arc_ori.getPotentials(),false);
		}
		gap = arc_ori.getGap();
		System.out.println("Solved with objective: "+arc_ori.getObjective());
		
		
//		Map<Activity,Integer> tensions = new HashMap<>();
//		tensions.putAll(cycle_red.getTensions());
//		sol = new Solution_TT(inst,red,tensions,true);
		
	}

}
