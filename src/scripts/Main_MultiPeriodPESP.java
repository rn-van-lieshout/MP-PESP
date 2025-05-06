package scripts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import ilog.concert.IloException;
import instance.Solution_TT;
import solversMultiPeriodPESP.SolverEPESPArcOriginal;
import solversMultiPeriodPESP.SolverEPESPArcReduced;
import solversMultiPeriodPESP.SolverEPESPCycleOriginal;
import solversMultiPeriodPESP.SolverEPESPCycleReduced;

public class Main_MultiPeriodPESP {

	public static void main(String[] args) throws IOException, IloException {
		String[] instances = {"Schweiz_Fernverkehr","toy_2","grid","Erding_NDP_S020","metroFixed","regional","Stuttgart"};

		int instanceCount = Integer.parseInt(args[0]);
		int solver = Integer.parseInt(args[1]); //1 = arc form. on PESP, 2 = cycle form on MP-PESP, 3 = arc form. on PESP, 4 = cycle form. on MP-PESP
		boolean usePrevSol = Boolean.parseBoolean(args[2]); //true means solving instances incrementally
		
//		int instanceCount = 0;
//		int solver = 4;
//		boolean usePrevSol = true;
//		
		
		System.out.println("Running instance "+instanceCount+" with solver "+solver);
        try (PrintWriter pw = new PrintWriter("results/"+instances[instanceCount]+"-"+solver+".txt")) {

			pw.println("name,status,objective,cpuTime,gap,nEvents,nArcs");
			Solution_TT prevSol = null;
			for(int transferProp = 0; transferProp<=10; transferProp++) {
				String name = instances[instanceCount]+"-EPESP-0."+transferProp;
				if(transferProp == 10) {
					name = instances[instanceCount]+"-EPESP-1.0";
				}
				PrintStream out = new PrintStream(new FileOutputStream("logs/"+name+"-solver"+solver+".txt"));
				System.setOut(out);
	            try (PrintWriter pwSol = new PrintWriter("solutions/"+name+"-"+solver+".txt")) {
					int cpu = 3600;
					if(solver == 1) {
						SolverEPESPArcOriginal sAO = new SolverEPESPArcOriginal(name, cpu);
						if(usePrevSol&&(prevSol!=null)) {
							sAO.setStartSolution(prevSol);
						}
						sAO.solve();
						sAO.print(pw,pwSol);
						prevSol = sAO.getSol();
					} else if(solver == 2) {
						SolverEPESPCycleOriginal sCO = new SolverEPESPCycleOriginal(name, cpu);
						if(usePrevSol&&(prevSol!=null)) {
							sCO.setStartSolution(prevSol);
						}
						sCO.solve();
						//sCO.printResults();
						sCO.print(pw,pwSol);
						prevSol = sCO.getSol();
					} else if(solver == 3) {
						SolverEPESPArcReduced sAR = new SolverEPESPArcReduced(name, cpu);
						if(usePrevSol&&(prevSol!=null)) {
							sAR.setStartSolution(prevSol);
						}
						sAR.solve();
						sAR.print(pw,pwSol);	
						prevSol = sAR.getSol();
					} else if(solver == 4) {
						SolverEPESPCycleReduced sCR = new SolverEPESPCycleReduced(name, cpu);
						if(usePrevSol&&(prevSol!=null)) {
							sCR.setStartSolution(prevSol);
						}
						sCR.solve();
						sCR.print(pw,pwSol);
						prevSol = sCR.getSol();
					}
				}
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
	}
}
