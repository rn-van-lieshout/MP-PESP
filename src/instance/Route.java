package instance;

import java.util.ArrayList;
import java.util.List;

import digraph.DirectedGraphArc;

public class Route {
	private OD od;
	private List<Activity_TT> arcs;
	
	private int minTime;
	private int maxTime;
	private int nTransfers; 
	
	public Route(OD od, List<DirectedGraphArc<Event_TT, Activity_TT>> orArcs, int transferPenalty) {
		super();
		this.od = od;
		this.arcs = new ArrayList<>();
		for(DirectedGraphArc<Event_TT, Activity_TT> a: orArcs) {
			Activity_TT att = a.getData();
			if(att.getType().equals("board")||att.getType().equals("alight")) {
				continue;
			}
			arcs.add(att);
		}
		minTime = 0; 
		maxTime = 0;
		nTransfers = 0;
		for(Activity_TT a: arcs) {
			minTime += a.getLb();
			if(a.getType().equals("change")) {
				minTime += transferPenalty;
				maxTime += a.getUb()+transferPenalty;
				nTransfers++;
			} else {
				maxTime += a.getLb();
			}
		}
	}
	
	public void printx() {
		System.out.println("\n Path from "+od.getOrigin()+" to "+od.getDestination() + " with "+nTransfers +" transfers and range ["+minTime+","+maxTime+"]");
		for(Activity_TT a: arcs) {
			System.out.println(a);
		}
	}

	public OD getOd() {
		return od;
	}
	
	public static boolean overlap(Route p1, Route p2) {
		//assumes p1<=p2
		if(p1.getMaxTime()>p2.getMinTime()) {
			return true;
		}
		return false;
	}
	
	public static List<Activity_TT> getSharedArcs(Route p1, Route p2) {
		List<Activity_TT> shared = new ArrayList<>();
		for(Activity_TT a1: p1.getArcs()) {
			if(p2.getArcs().contains(a1)) {
				shared.add(a1);
			}
		}
		return shared;
	}

	public List<Activity_TT> getArcs() {
		return arcs;
	}

	public int getMinTime() {
		return minTime;
	}

	public int getMaxTime() {
		return maxTime;
	}

	public int getnTransfers() {
		return nTransfers;
	}
	
	
	
}
