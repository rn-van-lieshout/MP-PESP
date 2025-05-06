package pesp;

import java.util.HashMap;
import java.util.Map;
import digraph.DirectedGraphArc;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import instance.Solution_TT;

public class ModelArcFormulation {

	private final EAN ean;
	
	private IloCplex model;
	private Map<Activity,IloNumVar> actToX;
	
	private Map<Event,IloNumVar> eventToPi;
	private Map<Activity,IloNumVar> actToZ;
	
	private String status;
	private double objective;
	private double gap;
	private Map<Activity,Integer> tensions;
	private Map<Event,Integer> potentials;
	
	public ModelArcFormulation(EAN ean, double constantTermObj) throws IloException {
		this.ean = ean;
		model = new IloCplex();
		initXAndObjective(constantTermObj);
		//initQ();
		initPi();
		initZ();
		addLinkingConstraints(false);
		//model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 1.0e-9);
		//model.setParam(IloCplex.DoubleParam.TimeLimit, 3600);
		//model.setParam(IloCplex.Param.WorkMem, 5000)
		model.setParam(IloCplex.Param.MIP.Limits.TreeMemory, 50000);
	}
	
	public boolean solve(long timeLimitPESP) throws IloException {
		model.exportModel("pesp.lp");
		//model.setOut(null);
		model.setParam(IloCplex.DoubleParam.TimeLimit, timeLimitPESP);
		boolean feasible = model.solve();
		tensions = new HashMap<>();
		objective = Double.MAX_VALUE;
		if(feasible) {
			objective = model.getObjValue();
			retrievePotentials();
			retrieveTensions();
		}
		System.out.println("Solved arc formulation with objective "+objective + "\n");
		gap = Double.MAX_VALUE;
		if(model.getStatus().equals(IloCplex.Status.Optimal)) {
			status = "optimal";
			gap = model.getMIPRelativeGap();
		} else if(model.getStatus().equals(IloCplex.Status.Feasible)) {
			status = "feasible";
			gap = model.getMIPRelativeGap();
		} else if(model.getStatus().equals(IloCplex.Status.Infeasible)) {
			status = "infeasible";
		} else {
			status = "noSolution";
		}
		
		model.clearModel();
		model.end();
		return feasible;
	}
	
	private void retrieveTensions() throws IloException {
		tensions = new HashMap<>();
		for(DirectedGraphArc<Event, Activity> arc: ean.getArcs()) {
			Activity act = arc.getData();
			double tensionval = model.getValue(actToX.get(act));
			int tensionInt = (int) Math.round(tensionval);
			tensions.put(arc.getData(),tensionInt);
		}
	}

	private void retrievePotentials() throws IloException {
		potentials = new HashMap<>();
		for(Event e: ean.getNodes()) {
			int time = (int) Math.round(model.getValue(eventToPi.get(e)));
			potentials.put(e, time);
		}
	}

	public double getGap() {
		return gap;
	}
	
	
	private void computeNodePotentials() throws IloException {
		model = new IloCplex();
		initPi();
		initZ();
		addLinkingConstraints(true);
		model.setOut(null);
		//model.exportModel("nodePotentials.lp");
		boolean feasible = model.solve();
		if(!feasible) {
			throw new Error("something is seriously wrong");
		}
		potentials = new HashMap<>();
		for(Event e: ean.getNodes()) {
			int time = (int) Math.round(model.getValue(eventToPi.get(e)));
			potentials.put(e, time);
		}
		model.clearModel();
		model.end();
		
	}
	
	private void initPi() throws IloException {
		boolean fixedOne = false;
		eventToPi = new HashMap<>(); 
		for(Event e: ean.getNodes()) {
			//earlier I set the bounds to 0<=pi<=T-1, but that gave an error
			//eventToPi.put(e,model.numVar(0,Double.MAX_VALUE));//e.getPeriod()-1)); //it might work better to declare this as a numVar (or remove the bounds altogether)
			//System.out.println("LCM: "+e.getPeriod());
			if(!fixedOne) {
				eventToPi.put(e,model.numVar(0,0));
				fixedOne = true;
			} else {
				eventToPi.put(e,model.numVar(0,e.getPeriod()-1));
			}
		}
	}
	
	private void initZ() throws IloException { 
		actToZ = new HashMap<>();
		for(DirectedGraphArc<Event, Activity> a: ean.getArcs()) {
			Activity act = a.getData();
			int lb = (int) Math.ceil((act.getLb()-a.getTo().getPeriod())/act.getPeriodicity());
			int ub = (int) Math.floor((act.getUb()+a.getFrom().getPeriod())/act.getPeriodicity()); 
			actToZ.put(act, model.intVar(lb,ub));
		}
	}
	
	private void addLinkingConstraints(boolean useSolution) throws IloException {
		for(DirectedGraphArc<Event, Activity> a: ean.getArcs()) {
			IloNumExpr lhs = model.diff(eventToPi.get(a.getTo()), eventToPi.get(a.getFrom()));
			lhs = model.sum(lhs,model.prod(actToZ.get(a.getData()), a.getData().getPeriodicity()));
			if(useSolution) {
				model.addEq(lhs, tensions.get(a.getData()));
			} else {
				model.addEq(lhs, actToX.get(a.getData()),"d"+a.getFrom().getId()+"-"+a.getTo().getId());
			}
		}

	}

	public double getObjective() {
		return objective;
	}

	private void initXAndObjective(double constantTermObj) throws IloException {
		actToX = new HashMap<>();
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

	public EAN getEAN() {
		return ean;
	}

	public Map<Activity, Integer> getTensions() {
		return tensions;
	}

	public Map<Event, Integer> getPotentials() {
		return potentials;
	}

	public String getStatus() {
		return status;
	}

	public void setStartSolution(Solution_TT startSolution) throws IloException {
		int nVars = actToX.size()+eventToPi.size();
		IloNumVar[] vars = new IloNumVar[nVars];
		double[] vals = new double[nVars];
		
		int i = 0;
		for(DirectedGraphArc<Event, Activity> arc: ean.getArcs()) {
			Activity a = arc.getData();
			vars[i] = actToX.get(a);
			vals[i] = startSolution.getTension(a);
			//model.addEq(vars[i], vals[i]);
			i++;
		}
		for(Event e: ean.getNodes()) {
			vars[i] = eventToPi.get(e);
			vals[i] = startSolution.getPotential(e);
			//model.addEq(vars[i], vals[i]);
			i++;
		}
		model.addMIPStart(vars,vals,IloCplex.MIPStartEffort.SolveMIP);
//		model.exportModel("submip.lp");
//		model.solve();
//		System.out.println("Solved start with solution:  "+model.getObjValue());
		//throw new Error("Stop here");
		
	}
	
	
	
	
	
	
}
