package solversIntegratedRouting;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import ilog.concert.IloException;
import instance.Instance_TT;
import instance.Solution_TT;

public abstract class Solver_TT {
	protected Instance_TT inst;
	protected int maxCPU;
	protected Solution_TT currentSol;
	protected Solution_TT bestSol;
	
	protected boolean smartRouting; //if true, only paths are taken into consideration if there is sufficient certainty that they will be shortest paths
	
	public Solver_TT(String name, int cpuTime, boolean smartRouting) throws IOException {
		inst = Instance_TT.readInstance(name);
		this.maxCPU = cpuTime;
		this.smartRouting = smartRouting;
	}
	
	public abstract void solve(int transferPercentage) throws IloException;
	
	public abstract void solveByComponent(int transferPercentage) throws IloException, FileNotFoundException;

	public abstract void computeRouting();
	
	public abstract void recomputeWeights();
	
	public void solveIteratively() throws FileNotFoundException, IloException {
		computeRouting();
		int round = 1;
		boolean continueSearch = true;
		int transferPercentage = 0;
		while(continueSearch) {
			System.out.println("\n Going for round "+round);
			solveByComponent(100);
			if(bestSol==null||currentSol.getTravelTime()<bestSol.getTravelTime()-0.001) {
				System.out.println("Sufficient improvement, go for next round");
				recomputeWeights();
				bestSol = currentSol;
				bestSol.printTimetable(new PrintWriter("data/"+inst.getName()+"/Timetable"+smartRouting+".csv"));
			} else {
				System.out.println("No improvement.");
				if(transferPercentage<10) {
					System.out.println("Adding transfer arcs");
					transferPercentage++;
				} else {
					System.out.println("Terminating.");
					continueSearch = false;
				}
			}
			round++;
		}
	}
	
	public void solveIterativelySteps() throws FileNotFoundException, IloException {
		computeRouting();
		int round = 0;
		int transferPercentage = 0;
		boolean continueSearch = true;
		while(continueSearch) {
			round++;
			if(transferPercentage<100) {
				transferPercentage += 10;
			}
			System.out.println("\n Going for round "+round +" with transferPercentage"+ transferPercentage);
			solveByComponent(transferPercentage);
			if(bestSol==null||currentSol.getTravelTime()<bestSol.getTravelTime()-0.001) {
				System.out.println("Sufficient improvement, go for next round");
				recomputeWeights();
				bestSol = currentSol;
				bestSol.printTimetable(new PrintWriter("data/"+inst.getName()+"/Timetable.csv"));
			} else {
				System.out.println("No improvement.");
				recomputeWeights();
				if(transferPercentage==100||round==20) {
					System.out.println("Terminating.");
					continueSearch = false;
				} 
			}
		}
	}
	
	public void printResults() {
		System.out.println("Solved instance with final traveltime: "+bestSol.getTravelTime());
	}
}
