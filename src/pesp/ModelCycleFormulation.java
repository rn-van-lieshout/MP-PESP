package pesp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import digraph.DirectedGraph;
import digraph.DirectedGraphArc;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import instance.Solution_TT;

public class ModelCycleFormulation {

	private final EAN ean;
	
	private Set<Cycle> cycleBasis; 
	private IloCplex model;
	private Map<Activity,IloNumVar> actToX;
	private Map<Cycle,IloNumVar> cycleToQ;
	private DirectedGraph<Event,Activity> mst;
	
	private String status;
	private double objective;
	private double gap;
	private Map<Activity,Integer> tensions;
	
	
	public ModelCycleFormulation(EAN ean, boolean useNiceTree, double constantTermObj) throws IloException {
		this.ean = ean;
		computeCycleBasis(useNiceTree);
		model = new IloCplex();
		initXAndObjective(constantTermObj);
		initQ(useNiceTree);
		//model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 1.0e-9);
		//model.setParam(IloCplex.DoubleParam.TimeLimit, 3600);
		model.setParam(IloCplex.Param.MIP.Limits.TreeMemory	, 50000);
		//setFeasibilityEmphasis();
	}

	public void setFeasibilityEmphasis() throws IloException {
		model.setParam(IloCplex.Param.Emphasis.MIP, IloCplex.MIPEmphasis.Feasibility);
	}
	
	private void computeCycleBasis(boolean useNiceTree) {
		cycleBasis = new HashSet<>();
		if(useNiceTree) {
			mst = ean.getNiceSpanningForest();//ean.getNiceSpanningTree();
			//mst = ean.getNiceSpanningTree();
			//System.out.println("HELLLLOO");
		} else {
			mst = ean.getMST();
		}
		System.out.println("Found minimum spanning forest with "+mst.getNumberOfNodes()+" events and "+mst.getNumberOfArcs()+ " activities");
		for(DirectedGraphArc<Event, Activity> arc: ean.getArcs()) {
			if(!mst.getArcs().contains(arc)) {
				//System.out.println("A: "+arc.getData());
				//Activity a = arc.getData();
				//we can find a cycle by adding a to the tree
				Cycle c = new Cycle(mst.getCycle(arc));
				cycleBasis.add(c);
				c.setGenerator(arc.getData());
			}
		}
		System.out.println("Size of cycle basis: "+cycleBasis.size());
		
		/*if(cycleBasis.size()!=ean.getNumberOfArcs()-ean.getNumberOfNodes()+1) {
			throw new IllegalArgumentException("The size of the cycle basis is not correct,"
					+ " assuming G is connected "+cycleBasis.size());
		}*/
	}
	
	public boolean solve(long timeLimitPESP) throws IloException {
		//model.exportModel("epespCycle.lp");
		//model.setOut(null);
		model.setParam(IloCplex.DoubleParam.TimeLimit, timeLimitPESP);
		boolean feasible = model.solve();
		tensions = new HashMap<>();
		objective = Double.MAX_VALUE;
		if(feasible) {
			objective = model.getObjValue();
			for(DirectedGraphArc<Event, Activity> arc: ean.getArcs()) {
				Activity act = arc.getData();
				double tensionval = model.getValue(actToX.get(act));
				int tensionInt = (int) Math.round(tensionval);
				tensions.put(arc.getData(),tensionInt);
			}
		}
		System.out.println("Solved cycle formulation with objective "+objective);
		
		gap = Double.MAX_VALUE;
		if(model.getStatus().equals(IloCplex.Status.Optimal)) {
			status = "optimal";
			gap = model.getMIPRelativeGap();
		} else if(model.getStatus().equals(IloCplex.Status.Feasible)) {
			status = "feasible";
			gap = model.getMIPRelativeGap();
			System.out.println("gap : "+gap);
		} else if(model.getStatus().equals(IloCplex.Status.Infeasible)) {
			status = "infeasible";
		} else {
			status = "noSolution";
		}
		
		model.clearModel();
		model.end();
		return feasible;
	}
	
	
	public double getObjective() {
		return objective;
	}
	
	public double getGap() {
		return gap;
	}

	private void initQ(boolean useNiceTree) throws IloException {
		cycleToQ = new HashMap<>();
		for(Cycle c: cycleBasis) {
			IloNumVar q_c = model.intVar(c.getLower(), c.getUpper());
			cycleToQ.put(c, q_c);
			IloNumExpr lhs = model.constant(0);
			for(Activity a: c.getForward()) {
				if(!actToX.containsKey(a)) {
					throw new Error("woopsie "+a);
				}
				lhs = model.sum(lhs,actToX.get(a));
			}
			for(Activity a: c.getBackward()) {
				lhs = model.diff(lhs,actToX.get(a));
			}
			if(useNiceTree) {
				model.addEq(lhs, model.prod(q_c, c.getGcd()));
			} else {
				model.addEq(lhs, model.prod(q_c, ean.getLCM()));
			}
		}
		
	}

	private void initXAndObjective(double constantTermObj) throws IloException {
		actToX = new HashMap<>();
		//System.out.println("Constant term: "+constantTermObj);
		IloNumExpr obj = model.constant(constantTermObj);
		for(DirectedGraphArc<Event, Activity> arc: ean.getArcs()) {
			Activity a = arc.getData();
			IloNumVar x_a = model.numVar(a.getLb(), a.getUb());
			actToX.put(a, x_a);
			//IloNumExpr slack = model.diff(x_a, a.getLb());
			obj = model.sum(obj,model.prod(a.getWeight(), x_a));
		}
		model.addMinimize(obj);
	}

	
	public DirectedGraph<Event, Activity> getMST() {
		return mst;
	}

	public EAN getEAN() {
		return ean;
	}

	public Map<Activity,Integer> getTensions() {
		return tensions;
	}

	public void setInitialSolution(Solution_TT reducedSol) throws IloException {
		List<IloNumVar> varsList = new ArrayList<>();
		List<Integer> valsList = new ArrayList<>();
		
		double wSum = 0;
		for (DirectedGraphArc<Event, Activity> arc : ean.getArcs()) {
		    Activity a = arc.getData();
		    Event from = arc.getFrom();
		    Event to = arc.getTo();
	        varsList.add(actToX.get(a));

		    if (!reducedSol.containsEvent(from)) {
		        //System.out.println("Does not contain " + from);
		    	//System.out.println("Weight of new act: "+a.getWeight());
		        valsList.add(reducedSol.getTensionReplace0(a));
		    } else if (!reducedSol.containsEvent(to)) {
		        //System.out.println("Does not contain " + to);
		    	//System.out.println("Weight of new act: "+a.getWeight());
		        valsList.add(reducedSol.getTensionReplace0(a));
		    } else {
		        valsList.add(reducedSol.getTension(a));
		        //model.addEq(actToX.get(a), reducedSol.getTension(a));
		        wSum += a.getWeight()*reducedSol.getTension(a);
		    }
		}
		//System.out.println("In starting sol function, wsum="+wSum);
		IloNumVar[] vars = varsList.toArray(new IloNumVar[0]);
		double[] vals = valsList.stream().mapToDouble(Integer::doubleValue).toArray();
		if(cycleToQ.size()>0) {
			model.addMIPStart(vars,vals,IloCplex.MIPStartEffort.SolveMIP);
		}
	}

	public String getStatus() throws IloException {
		return status;
	}	
	
}
