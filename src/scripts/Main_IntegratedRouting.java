package scripts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import ilog.concert.IloException;
import instance.Instance_TT;
import pesp.EAN;
import pesp.ModelArcFormulation;
import pesp.ModelCycleFormulation;
import solversIntegratedRouting.SolverArcOriginal;
import solversIntegratedRouting.SolverArcReduced;
import solversIntegratedRouting.SolverCycleOriginal;
import solversIntegratedRouting.SolverCycleReduced;

public class Main_IntegratedRouting {

	public static void main(String[] args) throws IOException, IloException {
		String[] instances = {"Schweiz_Fernverkehr","toy_2","grid","Erding_NDP_S020","metroFixed","regional","Stuttgart"};

		
		int instanceCount = 5;//Integer.parseInt(args[0]);
		boolean smartRouting = true;// Boolean.parseBoolean(args[1]); //if true, only paths are taken into consideration if there is sufficient certainty that they will be shortest paths
		int solver = 3;// Integer.parseInt(args[2]); //1 = arc form. on PESP, 2 = cycle form on MP-PESP, 3 = arc form. on PESP, 4 = cycle form. on MP-PESP
		
		String name = instances[instanceCount];
		PrintStream out = new PrintStream(new FileOutputStream("logs/"+name+"-TTPESP"+smartRouting+"-"+solver+".txt"));
		//System.setOut(out);
		
		int cpu = 3600;
		if(solver == 1) {
			SolverArcOriginal sAO = new SolverArcOriginal(name, cpu,smartRouting);
			sAO.solveIteratively();
			sAO.printResults();
		} else if(solver == 2) {
			SolverCycleOriginal sCO = new SolverCycleOriginal(name, cpu,smartRouting);
			sCO.solveIteratively();
			//sCO.solve();
			sCO.printResults();
		} else if(solver == 3) {
			SolverArcReduced sAR = new SolverArcReduced(name, cpu,smartRouting);
			sAR.solveIterativelySteps();
			sAR.printResults();
		} else if(solver == 4) {
			SolverCycleReduced sCR = new SolverCycleReduced(name, cpu,smartRouting);
			//sCR.solve();
			sCR.solveIterativelySteps();
			sCR.printResults();
		}
	}

}
