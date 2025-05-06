package instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digraph.DirectedGraph;


public class RoutingNetwork extends DirectedGraph<Event_TT, Activity_TT> {
	
	private static int boardingAlightTime = 10000;
	
	private Instance_TT inst;
	private List<Event_TT> stationNodes;
	private Map<Integer,Event_TT> stationIdToNode;
	
	public RoutingNetwork(Instance_TT inst) {
		super();
		this.inst = inst;
		for(Event_TT e: inst.getEvents()) {
			addNode(e);
		}
		for(Activity_TT a: inst.getActivities()) {
			if(a.getType().equals("drive")||a.getType().equals("wait")) {
				addArc(a.getFrom(),a.getTo(),a,a.getLb());
			} else if(a.getType().equals("change")) {
				// filter based on repetitions
				if(a.getFrom().getLine_freq_repetition()==1&&a.getTo().getLine_freq_repetition()==1) {
					addArc(a.getFrom(),a.getTo(),a,a.getLb()+inst.getTransferPenalty()); //fixed transfer penalty, independent of period
				}
			}
		}
		addStationNodesAndArcs();
	}
	
	public RoutingNetwork(Instance_TT inst, Solution_TT sol) {
		super();
		this.inst = inst;
		for(Event_TT e: inst.getEvents()) {
			addNode(e);
		}
		for(Activity_TT a: inst.getActivities()) {
			if(a.getType().equals("drive")||a.getType().equals("wait")) {
				addArc(a.getFrom(),a.getTo(),a,sol.getTension(a));
			} else if(a.getType().equals("change")) {
				// filter based on repetitions
				addArc(a.getFrom(),a.getTo(),a,sol.getTension(a)+inst.getTransferPenalty()); //fixed transfer penalty, independent of period
			}
		}
		addStationNodesAndArcs();
	}
	
	public void addStationNodesAndArcs() {
		
		stationNodes = new ArrayList<>();
		stationIdToNode = new HashMap<>();
		for(OD od: inst.getOdPairs()) {
			if(!stationIdToNode.containsKey(od.getOrigin())) {
				Event_TT sNode = new Event_TT(nodes.size()+1,"stationNode",od.getOrigin(),0,"",0,0);
				stationNodes.add(sNode); 
				stationIdToNode.put(od.getOrigin(), sNode);
				addNode(sNode);
			}
			if(!stationIdToNode.containsKey(od.getDestination())) {
				Event_TT sNode = new Event_TT(nodes.size()+1,"stationNode",od.getDestination(),0,"",0,0);
				stationNodes.add(sNode);
				stationIdToNode.put(od.getDestination(), sNode);
				addNode(sNode);
			}
		}
		
		for(Event_TT e: inst.getEvents()) {
			Event_TT sNode = stationIdToNode.get(e.getStop_id());
			if(sNode==null) {
				continue;
			}
			if(e.getType().equals("departure")) {
				//add arc from station node
				Activity_TT board = new Activity_TT(arcs.size()+1,sNode,e,inst.getGlobalPeriod(),inst.getGlobalPeriod(),0,"board");
				addArc(sNode,e,board,boardingAlightTime);
			} else if(e.getType().equals("arrival")) {
				//add arc to station node
				Activity_TT alight = new Activity_TT(arcs.size()+1,e,sNode,inst.getGlobalPeriod(),inst.getGlobalPeriod(),0,"alight");
				addArc(e,sNode,alight,boardingAlightTime);
			} else {
				throw new Error("HUH?");
			}
		}
	}

	public List<Event_TT> getStationNodes() {
		return stationNodes;
	}

	public Map<Integer, Event_TT> getStationIdToNode() {
		return stationIdToNode;
	}

	public static int getBoardingAlightTime() {
		return boardingAlightTime;
	}
	
	
	
	
}
