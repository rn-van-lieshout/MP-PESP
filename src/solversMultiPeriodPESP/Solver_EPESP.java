package solversMultiPeriodPESP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import ilog.concert.IloException;
import instance.Instance_TT;
import instance.Solution_TT;

public abstract class Solver_EPESP {
	protected Instance_TT inst;
	private String name;
	protected int maxCPU;
	
	protected Solution_TT startSolution;
	


	//results
	protected Solution_TT sol;
	protected String status;
	protected double cpuTime;
	protected double objective;
	protected double gap;
	protected int nEvents;
	protected int nArcs;
	
	public Solver_EPESP(String name, int cpuTime) throws IOException {
		this.name = name;
		inst = Instance_TT.readMPPESPInstance(name);
		this.maxCPU = cpuTime;
	}
	
	public abstract void solve() throws IloException;
	
	public void print(PrintWriter pw, PrintWriter pwSol) {
		pw.println(name+","+status+","+objective+","+cpuTime+","+gap+","+nEvents+","+nArcs);
		if(sol!=null) {
			sol.printTimetable(pwSol);
		}
	}

	public Solution_TT getSol() {
		return sol;
	}
	
	public void setStartSolution(Solution_TT startSolution) {
		this.startSolution = startSolution;
	}
	
	
}
