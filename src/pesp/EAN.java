package pesp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import digraph.DirectedGraph;
import digraph.DirectedGraphArc;
import instance.Instance_TT;
import util.Arithmetic;

public class EAN extends DirectedGraph<Event, Activity>
{
	
	private int LCM;
	private List<Integer> periods; //
	private boolean isHarmonic;
	private Map<Integer,List<DirectedGraph<Event,Activity>>> subgraphPerPeriod;
	private Map<Event,DirectedGraph<Event,Activity>> eventToSubgraph;
	
	private List<EAN> connectedComponentsEAN;
	
	private Event newEventNice;
	private List<Activity> newActivitiesNice;
	
	public EAN() {
		super();
	}

	
	public EAN(Instance_TT inst)
	{
		super();
		for(Event e: inst.getEvents()) {
			addNode(e);
		}
		for(Activity a: inst.getActivities()) {
			//only include the relevant activities
			if(!inst.isIrrelevant(a)) {
				addArc(a.getFrom(),a.getTo(),a,a.getSpan());
			} else {
				//System.out.println("Saw irelevant");
			}
		}
		determineConnectedComponentsEAN();
		if(getConnectedComponents().size()>1) {
			System.out.println("Instance can be decomposed!");
		}
	}
	
	public List<EAN> getConnectedComponentsEAN() {
		return connectedComponentsEAN;
	}
	
	public void determineConnectedComponentsEAN() {
		List<EAN> comps = new ArrayList<>();
		
		for(DirectedGraph<Event, Activity> g: getConnectedComponents()) {
			EAN comp = new EAN();
			for(Event e: g.getNodes()) {
				comp.addNode(e);
			}
			for(DirectedGraphArc<Event,Activity> a: g.getArcs()) {
				comp.addArc(a.getFrom(), a.getTo(), a.getData(), a.getCost());
			}
			comp.determineSubgraphPerPeriod();
			comp.determinePeriodsAndLCM();
			comps.add(comp);
		}
		System.out.println("There are  "+comps.size()+" components.");
		for(EAN ean: comps) {
			System.out.println("This one has lcm "+ean.getLCM()+" and periods: "+ean.periods +" and "+ean.getNumberOfNodes()+" nodes.");
		}
		connectedComponentsEAN = comps;
	}
	
	private void determinePeriodsAndLCM() {
		periods = new ArrayList<>();
		for(Event e: nodes) {
			if(!periods.contains(e.getPeriod())) {
				periods.add(e.getPeriod());
			}
		}
		Collections.sort(periods);
		int[] periodArray = new int[periods.size()];
		int i = 0;
		for(int p: periods) {
			periodArray[i] = p;
			i += 1;
		}
		LCM = Arithmetic.findLCM(periodArray);
		
		isHarmonic = true;
		for(Integer p1: periods) {
			for(Integer p2: periods) {
				if(p1<p2 &&p2%p1!=0) {
					isHarmonic = false;
				}
			}
		}
		//isHarmonic = true;
		if(isHarmonic) {
			System.out.println("Is harmonic "+periods);
		} else {
			System.out.println("Not harmonic "+periods);
		}
	}
	
	public void makeRootedPerComponent() {
		for(EAN component: this.connectedComponentsEAN) {
			if(!component.isHarmonic) {
				component.makeRooted();
				if(component.newEventNice!=null) {
					addNode(component.newEventNice);
				}
				for(Activity a: component.newActivitiesNice) {
					addArc(a.getFrom(),a.getTo(),a,a.getSpan());
				}
			} else {
				System.out.println("Component is harmonic, skipping");
			}
		}
		
	}

	public boolean isRooted() {
		if(isHarmonic) {
			return true;
		}
		
		
		//check if the lcm is a part of the period set
		if(!periods.contains(LCM)) {
			System.out.println("LCM not included -> not nice");
			return false;
		}
		
		//check if there is only a single component where T=L
		if(subgraphPerPeriod.get(LCM).size()>1) {
			System.out.println("EAN has more than one subgraph with LCM as period -> not nice");
			return false;
		}
		
		//check if all components are nice
		for(Integer per: periods) {
			if(per!=LCM) {
				for(DirectedGraph<Event,Activity> component: subgraphPerPeriod.get(per)) {
					if(!componentIsNice(component)) {
						System.out.println("Subgraph with period "+per +" is not nice");
						return false;
					}					
				}
			}
			
		}
		System.out.println("EAN is nice!");
		return true;
	}
	
	public void makeRooted() {
		System.out.println("Transforming instance to rooted instance");
		//check if the lcm is a part of the period set
		if(!periods.contains(LCM)) {
			//add an auxiliary event
			Event e = new Event(nodes.size()+1,LCM);
			addNode(e);
			newEventNice = e;
			System.out.println("Added event for LCM "+LCM);
		}
		determineSubgraphPerPeriod();
		newActivitiesNice = new ArrayList<>();
		//check if there is only a single component where T=L
		if(subgraphPerPeriod.get(LCM).size()>1) {
			//add arcs between these components
			int nComp = subgraphPerPeriod.get(LCM).size();
			
			//get some event in the first component
			Event e = subgraphPerPeriod.get(LCM).get(0).getNodes().get(0);
			//connect to events in the other components
			for(int i = 1; i<nComp; i++) {
				Event f = subgraphPerPeriod.get(LCM).get(i).getNodes().get(0);
				Activity a = new Activity(arcs.size()+1,e,f,0,LCM-1,0);
				newActivitiesNice.add(a);
				addArc(e,f,a,a.getSpan());
				System.out.println("Added arc to connect all LCM subgraphs");
			}
		}
		determineSubgraphPerPeriod();
		
		//check if all components are nice
		for(Integer per: periods) {
			if(per!=LCM) {
				for(DirectedGraph<Event,Activity> component: subgraphPerPeriod.get(per)) {
					if(!componentIsNice(component)) {
						//connect it to lcm
						Event e = component.getNodes().get(0);
						Event f = subgraphPerPeriod.get(LCM).get(0).getNodes().get(0);
						int periodicity = Arithmetic.gcd(e.getPeriod(), f.getPeriod());
						Activity a = new Activity(arcs.size()+1,e,f,0,periodicity-1,0); //changed ub from LCM to periodicity
						newActivitiesNice.add(a);
						addArc(e,f,a,a.getSpan());
						
						System.out.println("Added arc to connect subgraph  with period "+per + " to LCM with upper bound "+ (periodicity-1));
					}					
				}
			}
			
		}
		determineSubgraphPerPeriod();
	}
	
	/**
	 * Method that checks if a component is connected to an integer multiple
	 */
	private boolean componentIsNice(DirectedGraph<Event, Activity> component) {
		for(Event e: component.getNodes()) {
			//check if connection with outnode that is integer multiple
			for(Event f: getAdjacentNodes(e)) {
				//check if the period of f is a multiple of the period of e
				if(f.getPeriod()>e.getPeriod()&&f.getPeriod()%e.getPeriod()==0) {
					return true;
				}
			}
		}
		return false;
	}

	public void determineSubgraphPerPeriod() {
		determinePeriodsAndLCM();
		//System.out.println("Determining EAN per period");
		subgraphPerPeriod = new HashMap<>();
		eventToSubgraph = new HashMap<>();
		
		for(Integer per: periods) {
			//System.out.println("Determining period "+per);
			List<DirectedGraph<Event,Activity>> eanList = new ArrayList<>();
			EAN subEAN = new EAN();
			for(Event e: nodes) {
				if(e.getPeriod()==per) {
					subEAN.addNode(e);
				}
			}
			for(DirectedGraphArc<Event,Activity> a: arcs) {
				if(a.getFrom().getPeriod()==per && a.getTo().getPeriod()==per) {
					subEAN.addArc(a.getFrom(), a.getTo(), a.getData(), a.getData().getWeight());
				}
			}
			for(DirectedGraph<Event,Activity> component: subEAN.getConnectedComponents()) {
				eanList.add(component);
			}
			subgraphPerPeriod.put(per, eanList);
			
			//map every event to the right subgraph
			for(Event e: subEAN.getNodes()) {
				for(DirectedGraph<Event,Activity> subgraph: eanList) {
					if(subgraph.getNodes().contains(e)) {
						eventToSubgraph.put(e, subgraph);
						break;
					}
				}
			}
			
			//System.out.println(" There are "+eanList.size()+ " subgraphs with period "+per);
		}
	}

	public void addPeriod(Event e) {
		periods.add(e.getPeriod());
	}
	
	public DirectedGraph<Event,Activity> getNiceSpanningForest() {
		DirectedGraph<Event,Activity> niceForest = new DirectedGraph<Event,Activity>();
		determineConnectedComponentsEAN(); //added this to be sure. I think the TT-PESP needs this.
		for(EAN component: this.connectedComponentsEAN) {
			niceForest.addGraph(component.getNiceSpanningTree());
		}
		return niceForest;
	}
	
	public DirectedGraph<Event,Activity> getNiceSpanningTree() {
		//System.out.println("Getting nice spanning tree");
		DirectedGraph<Event,Activity> niceTree = new DirectedGraph<Event,Activity>();
		
		//first add the MSTs of all subgraphs induced by the different periods
		for(Integer per: periods) {
			//System.out.println("Period: "+ per);
			for(DirectedGraph<Event,Activity> component: subgraphPerPeriod.get(per)) {
				DirectedGraph<Event,Activity> tree_in_component = component.getMST();
				for(Event e: tree_in_component.getNodes()) {
					niceTree.addNode(e);
				}
				for(DirectedGraphArc<Event,Activity> a: tree_in_component.getArcs()) {
					niceTree.addArc(a.getFrom(),a.getTo(),a.getData(),a.getCost());
				}
			}
		}
		
		//add arcs between the subgraphs
		if(this.isHarmonic) {
			//determine the arcs that can still be added to the tree
			List<DirectedGraphArc<Event,Activity>> coTreeArcs = new ArrayList<>();
        	//coTreeArcs.sort(Comparator.comparingInt((DirectedGraphArc<Event,Activity> arc) -> arc.getData().getPeriodicity()).reversed());

        	for(DirectedGraphArc<Event,Activity> a: arcs) {
        		if(!eventToSubgraph.get(a.getFrom()).equals(eventToSubgraph.get(a.getTo()))) {
        			coTreeArcs.add(a);
        			//System.out.println("Co tree: "+a.getData());
        		}
        	}
        	//System.out.println("Component has "+arcs.size()+" arcs and "+coTreeArcs.size()+" cotree arcs");
        	coTreeArcs.sort(
        		    Comparator
        		        .comparingInt((DirectedGraphArc<Event, Activity> arc) -> arc.getData().getPeriodicity()).reversed()
        		        .thenComparingInt(arc -> (int) arc.getData().getSpan())
        		);
        	
        	//iterate over co-tree arcs, starting with largest periodicity, and add as long as possible
        	for(DirectedGraphArc<Event,Activity> a: coTreeArcs) {
        		if(niceTree.getCycle(a)==null) {
					niceTree.addArc(a.getFrom(),a.getTo(),a.getData(),a.getCost());
					System.out.println("Adding co-tree: "+a.getData());
        		}
        	}
	        
		} else {
			for(Integer per: periods) {
				if(per!=LCM) {
					int compCount = 1;
					for(DirectedGraph<Event,Activity> component: subgraphPerPeriod.get(per)) {
						//System.out.println("Adding arcs from period "+per + " component "+compCount);
						//we need to find an arc to the parent of component
						DirectedGraphArc<Event,Activity> arcToParent = determineArcToParent(component);
						niceTree.addArc(arcToParent.getFrom(), arcToParent.getTo(), arcToParent.getData(), arcToParent.getCost());
						compCount++;
					}

				}

			}
		}
		
		
		
		return niceTree;
	}
	
	/**
	 * Function that determines the parent of a given component. could be made more efficient
	 * @param component
	 * @return
	 */
	private DirectedGraphArc<Event, Activity> determineArcToParent(DirectedGraph<Event, Activity> component) {
		int periodChild = component.getNodes().get(0).getPeriod();
		int periodParent = LCM;
		double slack = Double.MAX_VALUE;
		DirectedGraphArc<Event, Activity> toAdd = null;
		//System.out.println("\n Looking for parent from child with period "+periodChild);
		for(Event e: component.getNodes()) {
			for(DirectedGraphArc<Event, Activity> out: getOutArcs(e)) {
				Event f = out.getTo();
				if(f.getPeriod()>periodChild&&f.getPeriod()%periodChild==0) {
					//period_f is integer multiple of period_e so candidate 
					if(f.getPeriod()<periodParent || f.getPeriod()==periodParent&&out.getCost()<slack) {
						periodParent = f.getPeriod();
						slack = out.getCost();
						//System.out.println("Setting slack to : "+slack);
						toAdd = out;
					}	
				}
			}
			for(DirectedGraphArc<Event, Activity> in: getInArcs(e)) {
				Event f = in.getFrom();
				if(f.getPeriod()>periodChild&&f.getPeriod()%periodChild==0) {
					//period_f is integer multiple of period_e so candidate 
					if(f.getPeriod()<periodParent || f.getPeriod()==periodParent&&in.getCost()<slack) {
						periodParent = f.getPeriod();
						slack = in.getCost();
						//System.out.println("Setting slack to : "+slack);
						toAdd = in;
					}	
				}
			}
		}
		return toAdd;
	}


	public int getLCM() {
		if(LCM==0) {
			throw new Error("Nooo");
		}
		return LCM;
	}
}
