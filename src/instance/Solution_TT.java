package instance;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digraph.Dijkstra;
import digraph.DirectedGraph;
import digraph.DirectedGraphArc;
import pesp.Activity;
import pesp.EAN;
import pesp.Event;
import util.Arithmetic;

public class Solution_TT {
	private Instance_TT inst;
	private Map<Activity,Integer> tensions; 	//perhaps I should switch these to Activity_TT?
	private Map<Event,Integer> potentials;
	
	private Map<Event_TT,Integer> potentials_TT;
	private Map<Activity,Integer> tensions_TT; 

	private RoutingNetwork rN;
	private double weightedDuration;
	private long travelTime;
	
	
	public Solution_TT(Instance_TT inst, Map<Activity, Integer> tensions, Map<Event, Integer> potentials,boolean computeRouting) {
		super();
		this.inst = inst;
		this.tensions = tensions;
		this.potentials = potentials;
		inferPotentials_TT();
		inferTensions();
		checkFeasibility();
		computeWeightedDuration();
		if(computeRouting) {
			travelTime = computeTravelTime();
		}
	}

	public Solution_TT(Instance_TT inst,EAN ean, Map<Activity, Integer> tensions, DirectedGraph<Event, Activity> mst, boolean computeRouting) {
		super();
		this.inst = inst;
		this.tensions = tensions;
		System.out.println("We have "+tensions.size()+ " tensions and "+ean.getNumberOfArcs()+" arcs");
		this.potentials = computePotentials(mst,ean);
		inferPotentials_TT();
		inferTensions();
		checkFeasibility();
		computeWeightedDuration();
		if(computeRouting) {
			travelTime = computeTravelTime();
		}
	}

	private void computeWeightedDuration() {
		weightedDuration = 0;
		for(Activity_TT a: inst.getActivities()) {
			weightedDuration += a.getWeight()*tensions_TT.get(a);
			//System.out.println("Wdur: "+weightedDuration);
		}
		System.out.println("Weighted duration is: "+weightedDuration);
	}

	public Solution_TT(Instance_TT inst, Solution_TT reducedSol) {
		this.inst = inst;
		boolean print = false;
		
		potentials_TT = new HashMap<>();
		for(List<Event_TT> startList: inst.getLineDirectionToStarts().values()) {
			//startList is a list of events that are start events of the same line in the same direction
			//should iterate over them and 
			int locPeriod = inst.getGlobalPeriod()/startList.size();
			if(print) {
				System.out.println("locPeriod: "+locPeriod);
			}
			int startPotential = reducedSol.getPotential(inst.getReducedEvent(startList.get(0)));
			int startCounter = 1;
			for(Event_TT start: startList) {
				potentials_TT.put(start, startPotential);
				Activity_TT succ_act = start.getSuccessor();
				int pot_prev = startPotential;
				if(print) {
					System.out.println("Start event "+startCounter+"/"+startList.size()+" has potential: "+pot_prev);
				}
				while(succ_act!=null) {
					if(print) {
						System.out.println("Succ act: "+succ_act+ " type="+succ_act.getType());
						System.out.println("Reduced act:  "+inst.getReducedActivity(succ_act));
					}
					
					int tension = reducedSol.getTension(inst.getReducedActivity(succ_act));
					if(print) {
						System.out.println("Reduced tension to next event is "+tension);
					}
					Event_TT succ_event = succ_act.getTo();
					//get the  potential of the successor in the reduced network'
					int pot_succ = reducedSol.getPotential(inst.getReducedEvent(succ_event));
					if(print) {
						System.out.println("Reduced potential is "+pot_succ);
					}
					//convert to lowest possible potential
					pot_succ =  Arithmetic.mod(pot_succ,locPeriod);
					if(print) {
						System.out.println("Change to "+pot_succ);
					}
					//increase until feasible
					while(!succ_act.isFeasible(tension, pot_prev, pot_succ)) {
						pot_succ += locPeriod;
						if(print) {
							System.out.println("Change in loop to "+pot_succ);
						}
						if(pot_succ>inst.getGlobalPeriod()) {
							throw new Error("Ooops");
						}
					}
					//store in map
					potentials_TT.put(succ_event, pot_succ);
					if(print) {
						System.out.println("Final potential is "+pot_succ+"\n");
					}
					//update variables and continue recursion
					succ_act = succ_event.getSuccessor();
					pot_prev = pot_succ;
				}
				startPotential = Arithmetic.mod(startPotential+locPeriod, inst.getGlobalPeriod());
				startCounter++;
			}
		}
		inferTensions();
		checkFeasibility();
		travelTime = computeTravelTime();	
	}

	public int getPotential(Event e) {
		return potentials_TT.get(e);
	}
	
	private void inferTensions() {
		tensions_TT = new HashMap<>();
		for(Activity_TT act: inst.getActivities()) {
			int pi_i = potentials_TT.get(act.getFrom());
			int pi_j = potentials_TT.get(act.getTo());
			int x = Arithmetic.mod(pi_j-pi_i,act.getPeriodicity());
			while(x<act.getLb()) {
				x += act.getPeriodicity();
			}
			tensions_TT.put(act, x);
		}
	}

	private void inferPotentials_TT() {
		potentials_TT = new HashMap<>();
		for(Event_TT e: inst.getEvents()) {
			//Event_TT eTT = (Event_TT) e;
			//System.out.println("Ev: "+e);
			potentials_TT.put(e, potentials.get(e));
		}
	}
	
	public void printTimetable(PrintWriter pw)  {		
		for(Event_TT e: inst.getEvents()) {
			int id = e.getId();
			int time = potentials_TT.get(e);
			pw.println(id+"; "+time);
			pw.flush();
		}
		pw.close();
	}
	
	private long computeTravelTime() {
		System.out.println("Computing routing on timetable");
		Map<Integer,List<OD>> originToODs = inst.getOriginToODs();
		long tt = 0;
		rN = new RoutingNetwork(inst,this);
		for(Event_TT origin: rN.getStationNodes()) {
			//skip origins without any passenger demand
			if(!originToODs.containsKey(origin.getStop_id())) {
				continue;
			}
			Dijkstra<Event_TT,Activity_TT> dijks = new Dijkstra<Event_TT,Activity_TT>(rN,origin);
			dijks.computeDistances();
			for(OD od: originToODs.get(origin.getStop_id())) {
				List<DirectedGraphArc<Event_TT,Activity_TT>> path = dijks.getPath(rN.getStationIdToNode().get(od.getDestination()));
				for(DirectedGraphArc<Event_TT,Activity_TT> dArc: path) {
					Activity_TT a = dArc.getData();
					if(a.getType().equals("board")||a.getType().equals("alight")) {
						continue;
					}
					a.incrementWeight(od.getPassengers());
					tt+= od.getPassengers()*dArc.getCost();
				}
			}
		}
		
		
		return tt;
	}

	
	private void checkFeasibility() {
		for(Activity_TT a: inst.getActivities()) {
			int x = tensions_TT.get(a);
			int pi_i = potentials_TT.get(a.getFrom());
			int pi_j = potentials_TT.get(a.getTo());
			if(!a.isFeasible(x,pi_i,pi_j)) {
				System.out.println(a.getFrom()+" and  "+a.getTo());
				System.out.println("x="+x+" pi_i="+pi_i+" pi_j="+pi_j);
				System.out.println(a);
				Activity_TT a_red = inst.getReducedActivity(a);
				System.out.println("Reduced: "+a_red);
				System.out.println("not feasible");
				throw new Error("Instance not feasible "+a.getType()+" id: "+a.getID());
			} 
		}
		
	}

	private Map<Event, Integer> computePotentials(DirectedGraph<Event, Activity> tree, EAN ean) {
		Map<Event, Integer> pot = new HashMap<>();
		
		//loop over components, do a graph traversal in each
		for(EAN comp: ean.getConnectedComponentsEAN()) {
			Event root = comp.getNodes().get(0);
			DirectedGraph<Event,Activity> tree_for_traversal = new DirectedGraph<Event,Activity>();;
			for(Event e: tree.getNodes()) {
				tree_for_traversal.addNode(e);
			}
			for(DirectedGraphArc<Event,Activity> a: tree.getArcs()) {
				//System.out.println(a.getData().t);
				int tension = tensions.get(a.getData());
				tree_for_traversal.addArc(a.getFrom(), a.getTo(), a.getData(), tension);
				//also add reverse arc
				tree_for_traversal.addArc(a.getTo(), a.getFrom(), a.getData(), -tension);
			}			
			Dijkstra<Event,Activity> dijks = new Dijkstra<Event,Activity>(tree_for_traversal,root);
			dijks.computeDistances();
			for(Event e: comp.getNodes()) {
				if(dijks.getDistance(e)==Double.MAX_VALUE) {
					throw new Error("No path to: "+e);
				}
				int dist = (int) Math.round(dijks.getDistance(e));
				int res = Arithmetic.mod(dist,e.getPeriod());
				pot.put(e, res);
			}
		}
		

		return pot;
	}

	public int getTension(Activity_TT a) {
		return tensions_TT.get(a);
	}
	
	public int getTension(Activity a) {
		if(tensions.containsKey(a)) {
			return tensions.get(a);
		}
		//recompute tension
		int pi_i = potentials.get(a.getFrom());
		int pi_j = potentials.get(a.getTo());
		int x = Arithmetic.mod(pi_j-pi_i,a.getPeriodicity());
		while(x<a.getLb()) {
			x += a.getPeriodicity();
		}
		return x;
	}
	
	public int getTensionReplace0(Activity a) {
		if(tensions.containsKey(a)) {
			return tensions.get(a);
		}
		//recompute tension
		int pi_i = 0;
		int pi_j = 0;
		if(containsEvent(a.getFrom()) ) {
			pi_i = potentials.get(a.getFrom());
		}
		if(containsEvent(a.getTo()) ) {
			pi_i = potentials.get(a.getTo());
		}
		int x = Arithmetic.mod(pi_j-pi_i,a.getPeriodicity());
		while(x<a.getLb()) {
			x += a.getPeriodicity();
		}
		return x;
	}

	public long getTravelTime() {
		return travelTime;
	}

	public RoutingNetwork getRoutingNetwork() {
		return rN;
	}

	public boolean containsEvent(Event from) {
		return potentials.containsKey(from);
	}
	
	
	
}
