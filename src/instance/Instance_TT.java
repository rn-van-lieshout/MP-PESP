package instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import digraph.Dijkstra;
import digraph.DijkstraTwoShortest;
import digraph.DirectedGraphArc;
import pesp.Activity;
import util.Arithmetic;
import util.Pair;

public class Instance_TT {
	private String name;
	private int globalPeriod;
	private int transferPenalty;
	private final List<Event_TT> events;
	private final List<Activity_TT> activities;
	private List<Activity_TT> irrelevantActivities; //these are not relevant for the PESP (free with weight below minWeight)
	private final List<OD> odPairs;
	private Map<Integer,List<OD>> originToODs;
	private int lowerBound;
	private int totTransferPenalty;
	private final double constantTerm; //term to make the pesp objective consistent with the MP-PESP objective
	
	private Map<Pair<Integer,String>,List<Event_TT>> lineDirectionToStarts;
	private Map<Event_TT,Event_TT> eventToReducedEvent; //map from every event to the reduced verions of that event
	private Map<Pair<Event_TT,Event_TT>,Activity_TT> actToReducedAct;
	//private Map<Pair<Event_TT,Event_TT>,Activity_TT> eventPairToActList; //map from reduced events to all activities corresponding to that pair

	
	
	public Instance_TT(String name, int globalPeriod, int transferPenalty, List<Event_TT> events,
			List<Activity_TT> activities, List<OD> odPairs,double constantTerm) {
		super();
		this.name = name;
		this.globalPeriod = globalPeriod;
		this.transferPenalty = transferPenalty;
//		this.transferPenalty = 0;
//		System.out.println("setting transfer penalty to 0");
		this.events = events;
		this.activities = activities;
		this.odPairs = odPairs;
		if(odPairs!=null) {
			originToODs = new HashMap<>();
			for(OD od: odPairs) {
				if(originToODs.containsKey(od.getOrigin())) {
					originToODs.get(od.getOrigin()).add(od);
				} else {
					List<OD> ods = new ArrayList<>();
					ods.add(od);
					originToODs.put(od.getOrigin(), ods);
				}
			}
		}
		this.constantTerm = constantTerm;
		
		determineStartEvents();
		System.out.println("We have an instance with "+events.size()+" events and "+activities.size()+" activities. Transferpenalty: " + transferPenalty);		
	}
	
	
	private void determineStartEvents() {
		lineDirectionToStarts = new HashMap<>();
		
		for(Event_TT e: events) {
			if(e.getPredecessor()==null) {
				Pair<Integer,String> pair = new Pair<>(e.getLine_id(),e.getLine_direction());
				if(!lineDirectionToStarts.containsKey(pair)) {
					lineDirectionToStarts.put(pair, new ArrayList<>());
				}
				lineDirectionToStarts.get(pair).add(e);
			}
		}
	}
	
	public void writeEPESPinstance() throws FileNotFoundException {
		String ori = name.substring(0, name.length()-8);
		
		List<Activity_TT> transfers = new ArrayList<>();
		for(Activity_TT a: activities) {
			if(a.getType().equals("change")&&a.getWeight()>0) {
				transfers.add(a);
			}
		}
		transfers.sort(Comparator.comparingDouble(Activity_TT::getWeight).reversed());
		
		for(int i = 0; i<=10; i++) {
			//create 10 instances with varying number of transfer activities
			String folderPath = "data/"+ori+"-EPESP-0."+i; // Change the path accordingly
			if(i==10) {
				folderPath = "data/"+ori+"-EPESP-1.0";
			}
	        File folder = new File(folderPath);

	        if (!folder.exists()) {
	            if (folder.mkdir()) {
	                System.out.println("Folder created successfully!");
	            } else {
	                System.out.println("Failed to create folder.");
	            }
	        } else {
	            System.out.println("Folder already exists.");
	        }
			//copy config
	        Path sourcePath = Paths.get("data/"+ori+"/Config.csv"); 
	        Path targetPath = Paths.get(folderPath+"/Config.csv"); 

	        try {
	            // Copy file from source to target
	            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
	            System.out.println("File copied successfully!");
	        } catch (IOException e) {
	            System.err.println("Error copying file: " + e.getMessage());
	        }
	        
			PrintWriter pwE = new PrintWriter(folderPath+"/Events.csv");
			pwE.println("event_id; type; stop_id; line_id; line_direction; period");
			Map<Integer,Integer> oldToNewID = new HashMap<>();
			int eCount = 1;
			for(Event_TT e: events) {
				pwE.println(eCount+"; \""+e.getType()+"\"; "+e.getStop_id()+"; "+e.getLine_id()+"; "+e.getLine_direction()+"; "+e.getPeriod());
				pwE.flush();
				oldToNewID.put(e.getId(), eCount);
				eCount++;
			}
			pwE.close();
			
			PrintWriter pwA = new PrintWriter(folderPath+"/Activities.csv");
			pwA.println("activity_index; type; from_event; to_event; lower_bound; upper_bound; weight");
			int actCount = 1;
			for(Activity_TT a: activities) {
				if(a.getType().equals("change")) {
					continue;
				}
				//# activity_index	 type	 from_event	 to_event	 lower_bound	 upper_bound
				int fromID = oldToNewID.get(a.getFrom().getId());
				int toID = oldToNewID.get(a.getTo().getId());
				pwA.println(actCount+"; \""+a.getType()+"\"; "+fromID+"; "+toID+"; "+a.getLb()+"; "+a.getUb()+"; "+a.getWeight());
				actCount++;
				pwA.flush();
			}
			//add a percentage of the transfers
			int transferCount = 0;
			for(Activity_TT a: transfers) {
				//# activity_index	 type	 from_event	 to_event	 lower_bound	 upper_bound
				if(transferCount/(transfers.size()+0.0)>=i/10.0) {
					break;
				}
				int fromID = oldToNewID.get(a.getFrom().getId());
				int toID = oldToNewID.get(a.getTo().getId());
				pwA.println(actCount+"; \""+a.getType()+"\"; "+fromID+"; "+toID+"; "+a.getLb()+"; "+a.getUb()+"; "+a.getWeight());
				actCount++;
				pwA.flush();
				transferCount++;
			}
			pwA.close();
			System.out.println(folderPath + " has "+transferCount+"/"+transfers.size()+" transfers.");
		}
		

	}

	/**
	 * Method that resets the activity weights and recomputes them based on an input routing
	 * @param rN
	 */
	public void recomputeWeights(RoutingNetwork rN) {
		for(Activity_TT a: activities) {
			a.setWeight(0);
		}
		routeAndSetActivityWeights(rN);
	}
	
	private void routeAndSetActivityWeights(RoutingNetwork rN) {
		lowerBound = 0;
		totTransferPenalty = 0;
		for(Event_TT origin: rN.getStationNodes()) {
			//skip origins without any passenger demand
			if(!originToODs.containsKey(origin.getStop_id())) {
				continue;
			}
			Dijkstra<Event_TT,Activity_TT> dijks = new Dijkstra<Event_TT,Activity_TT>(rN,origin);
			dijks.computeDistances();
			//System.out.println("Origin: "+origin);
			for(OD od: originToODs.get(origin.getStop_id())) {
				List<DirectedGraphArc<Event_TT,Activity_TT>> path = dijks.getPath(rN.getStationIdToNode().get(od.getDestination()));
				for(DirectedGraphArc<Event_TT,Activity_TT> dArc: path) {
					Activity_TT a = dArc.getData();
					if(a.getType().equals("board")||a.getType().equals("alight")) {
						continue;
					} else if(a.getType().equals("change")) {
						totTransferPenalty+= od.getPassengers()*transferPenalty;
					}
					a.incrementWeight(od.getPassengers());
					lowerBound+= od.getPassengers()*dArc.getCost();
				}
			}
		}
	}
	
	/**
	 * Method that aims finding a "smarter" routing by computing the two shortest paths, and only incrementing the weights if there is sufficient certainty that the first path is really shorter
	 * @param rN
	 */
	private void routeAndSetActivityWeightsNovel(RoutingNetwork rN) {
		totTransferPenalty = 0;
		int nPaths = 2; 
		for(Event_TT origin: rN.getStationNodes()) {
			//skip origins without any passenger demand
			if(!originToODs.containsKey(origin.getStop_id())) {
				continue;
			}
			DijkstraTwoShortest<Event_TT,Activity_TT> dijks = new DijkstraTwoShortest<Event_TT,Activity_TT>(rN,origin,nPaths);
			dijks.computeDistances();
			System.out.println("Origin: "+origin);
			for(OD od: originToODs.get(origin.getStop_id())) {
				
				List<List<DirectedGraphArc<Event_TT,Activity_TT>>> paths = dijks.getPaths(rN.getStationIdToNode().get(od.getDestination()));
				//System.out.println("We have "+paths.size()+" paths");
				Route p1 = new Route(od,paths.get(0),transferPenalty);
				Route p2 = new Route(od,paths.get(1),transferPenalty);
				//Route p3 = new Route(od,paths.get(paths.size()-1));

				boolean distribute = false;
				if(Route.overlap(p1, p2)) {
					//p1.print();
					//p2.print();
					if(p1.getMinTime()==p2.getMinTime()&&p1.getMaxTime()>p2.getMaxTime()) {
						//switch roles
						p1 = new Route(od,paths.get(1),transferPenalty);
						p2 = new Route(od,paths.get(0),transferPenalty);
					}
					
					if(p1.getMinTime()==p2.getMinTime()) {
						distribute = true;
						for(Activity_TT a: p1.getArcs()) {
							if(!a.getType().equals("change")&&!Route.getSharedArcs(p1, p2).contains(a)) {
								a.incrementWeight(od.getPassengers()/2.0);
							}
						}
						for(Activity_TT a: p2.getArcs()) {
							if(!a.getType().equals("change")&&!Route.getSharedArcs(p1, p2).contains(a)) {
								a.incrementWeight(od.getPassengers()/2.0);
							}
						}
					}
				}
				
				
				if(!distribute) {
					for(Activity_TT a: p1.getArcs()) {
						if(a.getType().equals("board")||a.getType().equals("alight")) {
							continue;
						} else if(a.getType().equals("change")) {
							totTransferPenalty+= od.getPassengers()*transferPenalty;
						}
						a.incrementWeight(od.getPassengers());
					}
				}
			}
		}
	}


	public void computeRouting(boolean novelApproach) {		
		RoutingNetwork rN = new RoutingNetwork(this);
		if(novelApproach) {
			this.routeAndSetActivityWeightsNovel(rN);
		} else {
			routeAndSetActivityWeights(rN);
		}
	}
	
	public void ignoreFreeActivities(int transferPercentage) {
		
		List<Activity_TT> transfers = new ArrayList<>();
		for(Activity_TT a: activities) {
			if(a.getType().equals("change")&&a.getWeight()>0) {
				transfers.add(a);
			}
		}
		transfers.sort(Comparator.comparingDouble(Activity_TT::getWeight).reversed());
		
		irrelevantActivities = new ArrayList<>();
		int transferCount = 0;
		for(Activity_TT a: transfers) {
			if(transferCount/(transfers.size()+0.0)>transferPercentage/100.0) {
				this.irrelevantActivities.add(a);
				//System.out.println("Ignoring: "+a);
			}	
			transferCount++;
		}
		System.out.println("Ignoring "+irrelevantActivities.size()+"/ "+transferCount+" free activities with percentage = "+transferPercentage);
		//activities.removeAll(toRemove);
	}
	
	public void printStats() {
		int nEvents = events.size();
		int nActivities = activities.size();
		int nChange = 0;
		int nHeadway = 0;
		int nDrive = 0;
		int nWait = 0;
		int nSync = 0;
		int nChangeFirst = 0;
		for(Activity_TT a: activities) {
			if(a.getType().equals("drive")) {
				nDrive++;
			} else if(a.getType().equals("wait")) {
				nWait++;
			} else if(a.getType().equals("headway")) {
				nHeadway++;
			} else if(a.getType().equals("sync")) {
				nSync++;
			} else if(a.getType().equals("change")) {
				nChange++;
				if(a.getFrom().getLine_freq_repetition()==1&&a.getTo().getLine_freq_repetition()==1) {
					nChangeFirst++;
				}
			}
		}
		System.out.println("Instance with "+nEvents+" events, "+nActivities+" activities");
		System.out.println(nDrive+" drive, "+nWait + " wait, "+nChange+" change ("+nChangeFirst+" between firsts), "+nHeadway+" headway, "+nSync+" sync.");
		System.out.println("Lowerbound : "+lowerBound);
	}
	
	/**
	 * For the MP-PESP to work, we need that if there exists a transfer or headway activity between the i'th and j'th occurence of some event, it should also exist between other occurrences
	 * Here we add activities to make this work
	 * @return
	 */
	public boolean completeActivities() {
		Map<Event_TT,List<Event_TT>> eventToOccurrences = getEventToRepetition();
		Map<Event_TT,Event_TT> eventToFirstOccurrence = getEventToFirst(eventToOccurrences);

		//eventPairToActList = new HashMap<>();
		
		List<Activity_TT> toAdd = new ArrayList<>();
		int actId = activities.size()+1;
		int count = 0;
		for(Activity_TT a: activities) {
			if(count%1000==0) {
				System.out.println("count: "+count +" added : "+toAdd.size());
			}
			count++;
			if(!a.getType().equals("headway")&&!a.getType().equals("change")) {
				continue;
			}
			Event_TT from = a.getFrom();
			Event_TT to = a.getTo();
//			System.out.println("\n \n from: "+from);
//			System.out.println("to: "+to);
			
			List<Event_TT> occ_from_list = eventToOccurrences.get(eventToFirstOccurrence.get(from));
			List<Event_TT> occ_to_list = eventToOccurrences.get(eventToFirstOccurrence.get(to));
			
			//System.out.println("occ_to_list: "+occ_to_list);
			
			for(Event_TT occ_from: occ_from_list) {
				for(Event_TT occ_to: occ_to_list) {
//					System.out.println("\n occ_from: "+occ_from);
//					System.out.println("occ_to: "+occ_to);
					//check if you can find same activity
					boolean foundActivity = false;
					for(Activity_TT a2: activities) {
						if(a2.getFrom().equals(occ_from)&&a2.getTo().equals(occ_to)) {
							if(a2.getType().equals(a.getType())&&a.getLb()==a2.getLb()&&a.getUb()==a2.getUb()) {
								//found activity
								foundActivity = true;
								break;
							}
						}
					}
					if(!foundActivity) {
						//System.out.println("Did not find "+a.getType()+" "+a);
						Activity_TT addAct = new Activity_TT(actId,occ_from,occ_to,a.getLb(),a.getUb(),0,a.getType());
						actId++;
						toAdd.add(addAct);
						//throw new Error("");
					} 
				}
			}
		}
		System.out.println("Added "+toAdd.size()+" activities to complete instance");
		activities.addAll(toAdd);
		return (toAdd.size()>0);
	}
	
	public Instance_TT getPESPfromMPPESP() {
		List<Event_TT> eventsPESP = new ArrayList<>();
		Map<Event_TT,List<Event_TT>> eventToOccurrences = new HashMap<>();
		int id = 1;
		for(Event_TT e: events) {
			int occurrences = globalPeriod/e.getPeriod();
			eventToOccurrences.put(e, new ArrayList<>());
			for(int i = 1; i<=occurrences; i++) {
				// String type, int stop_id, int line_id, String line_direction, int line_freq_repetition, int period
				Event_TT e_i = new Event_TT(id,e.getType(),e.getStop_id(),e.getLine_id(),e.getLine_direction(),i,globalPeriod);
				id++;
				eventsPESP.add(e_i);
				eventToOccurrences.get(e).add(e_i);
			}
		}
		
		List<Activity_TT> activitiesPESP = new ArrayList<>();
		int actId = 1;
		double constantTerm = 0; //we need to correct the objective if we use PESP
		for(Activity_TT a: activities) {
			//check for type
			if(a.getType().equals("drive")||a.getType().equals("wait")) {
				int occurrences = globalPeriod/a.getFrom().getPeriod();
				//need to add one activity for each occurrence
				for(int i = 1; i<=occurrences; i++) {
					Event_TT from_i = eventToOccurrences.get(a.getFrom()).get(i-1);
					Event_TT to_i = eventToOccurrences.get(a.getTo()).get(i-1);
					//dividing the weight over the activities
					Activity_TT a_i = new Activity_TT(actId,from_i,to_i,a.getLb(),a.getUb(),a.getWeight()/occurrences,a.getType());
					actId++;
					activitiesPESP.add(a_i);
				}
			} else if(a.getType().equals("change")) {
				//check if transfer should be ignored
				List<Event_TT> occ_list_from = eventToOccurrences.get(a.getFrom());
				List<Event_TT> occ_list_to = eventToOccurrences.get(a.getTo());
				int lb = a.getLb();
				int ub = globalPeriod+lb-1; 
				double weight = a.getWeight()/(occ_list_from.size()*occ_list_to.size());
				for(Event_TT from_i: occ_list_from) {
					for(Event_TT to_i: occ_list_to) {
						Activity_TT a_i = new Activity_TT(actId,from_i,to_i,lb,ub,weight,a.getType());
						actId++;
						activitiesPESP.add(a_i);
					}
				}
				double correction = (globalPeriod-a.getPeriodicity())/2.0;
				constantTerm = constantTerm - correction*a.getWeight();
			} else if(a.getType().equals("headway")) {
				List<Event_TT> occ_list_from = eventToOccurrences.get(a.getFrom());
				List<Event_TT> occ_list_to = eventToOccurrences.get(a.getTo());
				int lb = a.getLb();
				int ub = globalPeriod-lb; 
				//add between all combinations
				for(Event_TT from_i: occ_list_from) {
					for(Event_TT to_i: occ_list_to) {
						Activity_TT a_i = new Activity_TT(actId,from_i,to_i,lb,ub,0,a.getType());
						actId++;
						activitiesPESP.add(a_i);
					}
				}
				
			} else {
				throw new Error("no support for "+a.getType());
			}
		}
		
		//add the sync activities
		for(Event_TT e: events) {
			int occurrences = globalPeriod/e.getPeriod();
			if(occurrences==1) {
				//don't need sync
				continue;
			}
			Event_TT from = eventToOccurrences.get(e).get(0);
			for(int i = 1; i<=occurrences-1; i++) {
				Event_TT to = eventToOccurrences.get(e).get(i);
				int timeBetween = this.globalPeriod/occurrences*i;
				Activity_TT a_i = new Activity_TT(actId,from,to,timeBetween,timeBetween,0,"sync");
				actId++;
				activitiesPESP.add(a_i);
			}
		}
		System.out.println("Constant term is in inst: "+constantTerm);
		return new Instance_TT(name+"-PESP",globalPeriod,transferPenalty,eventsPESP,activitiesPESP,odPairs,constantTerm);
	}


	/**
	 * Method that keeps only one event in case they occur multiple times per period
	 * @return
	 */
	public Instance_TT getMPPESPfromPESP() {
		
		Map<Event_TT,List<Event_TT>> eventToOccurrences = getEventToRepetition();
		Map<Event_TT,Event_TT> eventToFirstOccurrence = getEventToFirst(eventToOccurrences);
		if(syncActivitiesBetweenDifferentEvents(eventToFirstOccurrence,eventToOccurrences)) {
			throw new Error("not good");
		}
		//could also check if the bounds are the same for different occurrences
		
		//create all reduced all events
		List<Event_TT> events_reduced = new ArrayList<>();
		Map<Event_TT,Event_TT> firstToReduced = new HashMap<>();
		eventToReducedEvent = new HashMap<>();
		actToReducedAct = new HashMap<>();
		for(Event_TT first: eventToOccurrences.keySet()) {
			int period = globalPeriod/eventToOccurrences.get(first).size();
			Event_TT reducedEvent = new Event_TT(first.getId(),first.getType(),first.getStop_id(),first.getLine_id(),first.getLine_direction(),first.getLine_freq_repetition(),period);
			events_reduced.add(reducedEvent);
			firstToReduced.put(first, reducedEvent);
			
			for(Event_TT occurence: eventToOccurrences.get(first)) { 
				eventToReducedEvent.put(occurence, reducedEvent);
			}
		}
		
		//create all reduced activities
		List<Activity_TT> activities_reduced = new ArrayList<>();
		for(Activity_TT a: activities) {
			Event_TT from = a.getFrom();
			Event_TT to = a.getTo();
			if(from.getLine_freq_repetition()!=1||to.getLine_freq_repetition()!=1) {
				//do not add
				continue;
			}
			Event_TT fromReduced = firstToReduced.get(from);
			Event_TT toReduced = firstToReduced.get(to);
			if(a.getType().equals("drive")||a.getType().equals("wait")) { 
				//add activity with the same characteristics for the reduced events
				Activity_TT aReduced = new Activity_TT(a.getID(),fromReduced,toReduced,a.getLb(),a.getUb(),a.getWeight(),a.getType());
				activities_reduced.add(aReduced);
				actToReducedAct.put(new Pair<>(fromReduced,toReduced),aReduced);
			} else if(a.getType().equals("change")) {
				//modify the upper bound
				int lb = a.getLb();
				int newUb = lb+Arithmetic.gcd(fromReduced.getPeriod(), toReduced.getPeriod())-1;
				//add activity with the same characteristics for the reduced events
				Activity_TT aReduced = new Activity_TT(a.getID(),fromReduced,toReduced,lb,newUb,a.getWeight(),a.getType());
				activities_reduced.add(aReduced);
				actToReducedAct.put(new Pair<>(fromReduced,toReduced),aReduced);
			}
			else if(a.getType().equals("headway")) {
				//modify the upper bound
				int lb = a.getLb();
				int newUb = Arithmetic.gcd(fromReduced.getPeriod(), toReduced.getPeriod())-lb;
				//add activity with the same characteristics for the reduced events
				Activity_TT aReduced = new Activity_TT(a.getID(),fromReduced,toReduced,lb,newUb,a.getWeight(),a.getType());
				activities_reduced.add(aReduced);
				actToReducedAct.put(new Pair<>(fromReduced,toReduced),aReduced);
			} else {
				System.out.println("ignore Turnaround "+a);
				//throw new Error("Other type? "+a.getType()+" "+(a.getType().equals("drive"))+a);
			}
		}
		
		//check if the bounds of the original instance match those in the reduced one
		/*System.out.println("\n Checking consistency");
		for(Activity_TT a_red: activities_reduced) {
			Event_TT from = a_red.getFrom();
			Event_TT to = a_red.getTo();
			for(Activity_TT a: activities) {
				if(eventToFirstOccurrence.get(a.getFrom()).equals(from)&&eventToFirstOccurrence.get(a.getTo()).equals(to)) {
					if(a.getLb()!=a_red.getLb()||a.getUb()!=a_red.getUb()) {
						System.out.println("\n a_red: "+a_red);
						System.out.println("a: "+a + " type: "+a.getType());
						//throw new Error("Not consistent");
					}
				}
			}
		}
		System.out.println("Finish checking consistency");*/
		
		return new Instance_TT(name+"-reduced",globalPeriod,transferPenalty,events_reduced,activities_reduced,odPairs,0);
	}
	
	public Activity_TT getReducedActivity(Activity_TT act) {
		Event_TT redFrom = eventToReducedEvent.get(act.getFrom());
		Event_TT redTo = eventToReducedEvent.get(act.getTo());
		return actToReducedAct.get(new Pair<>(redFrom,redTo));
	}
	
	public Event_TT getReducedEvent(Event_TT e) {
		return eventToReducedEvent.get(e);
	}


	private Map<Event_TT, Event_TT> getEventToFirst(Map<Event_TT,List<Event_TT>> eventToOccurrences) {
		Map<Event_TT, Event_TT> eventToFirst = new HashMap<>();
		for(Entry<Event_TT,List<Event_TT>> ent: eventToOccurrences.entrySet()) {
			Event_TT first = ent.getKey();
			for(Event_TT other: ent.getValue()) {
				eventToFirst.put(other, first);
			}
		}
		return eventToFirst;
	}


	private boolean syncActivitiesBetweenDifferentEvents(Map<Event_TT, Event_TT> eventToFirst, Map<Event_TT, List<Event_TT>> eventToOccurrences) {
		for(Activity_TT a: activities) {
			if(a.getType().equals("sync")) {
				Event_TT from = a.getFrom();
				Event_TT to = a.getTo();
				Event_TT fromFirst = eventToFirst.get(from);
				Event_TT toFirst = eventToFirst.get(to);
				if(!fromFirst.equals(toFirst) ) {
					System.out.println("sync between: "+from+" and "+to);
					System.out.println("from first: "+fromFirst);
					System.out.println("to first: "+toFirst);
					return true;
				}
			}
		}
		return false;
	}


	public Map<Event_TT,List<Event_TT>> getEventToRepetition() {
		Map<Event_TT,List<Event_TT>> eventToOccurrences = new HashMap<>();
		for(Event_TT e: events) {
			if(e.getLine_freq_repetition()==1) {
				//found a new event
				List<Event_TT> occurrences = new ArrayList<>();
				occurrences.add(e);
				eventToOccurrences.put(e,occurrences);
			} else {
				//find first occurrence
				boolean foundFirst = false;
				for(Event_TT first: eventToOccurrences.keySet()) {
					if(e.sameEventExceptRepetition(first)) {
						eventToOccurrences.get(first).add(e);
						foundFirst = true;
						break;
					}
				}
				if(!foundFirst) {
					throw new Error("Did not find first");
				}
			}
		}
		return eventToOccurrences;
	}

	public String getName() {
		return name;
	}

	public int getGlobalPeriod() {
		return globalPeriod;
	}

	public int getTransferPenalty() {
		return transferPenalty;
	}

	public List<Event_TT> getEvents() {
		return events;
	}

	public List<Activity_TT> getActivities() {
		return activities;
	}

	public List<OD> getOdPairs() {
		return odPairs;
	}
	
	public Map<Integer, List<OD>> getOriginToODs() {
		return originToODs;
	}
	
	


	public Map<Pair<Integer, String>, List<Event_TT>> getLineDirectionToStarts() {
		return lineDirectionToStarts;
	}


	public static Instance_TT readInstance(String name) throws IOException {
		//read config file
		String prefix = "data/"+name+"/";
		int globalPeriod = 0;
		int transferPenalty;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(prefix+"Config.csv"));
		String line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		String[] data = line.split("; ");
		globalPeriod = Integer.parseInt(data[1]);
		line = bufferedReader.readLine();
		data = line.split("; ");
		transferPenalty = Integer.parseInt(data[1]);
		bufferedReader.close();
		
		List<Event_TT> events = Instance_TT.readEvents(name,globalPeriod,false);
		List<Activity_TT> activities = Instance_TT.readActivities(name,events,false);
		List<OD> ods = Instance_TT.readODs(name);
		return new Instance_TT(name,globalPeriod,transferPenalty,events,activities,ods,0);
	}
	
	public static Instance_TT readMPPESPInstance(String name) throws IOException {
		//read config file
		String prefix = "data/"+name+"/";
		int globalPeriod = 0;
		int transferPenalty;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(prefix+"Config.csv"));
		String line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		String[] data = line.split("; ");
		globalPeriod = Integer.parseInt(data[1]);
		line = bufferedReader.readLine();
		data = line.split("; ");
		transferPenalty = Integer.parseInt(data[1]);
		bufferedReader.close();
		
		List<Event_TT> events = Instance_TT.readEvents(name,globalPeriod,true);
		List<Activity_TT> activities = Instance_TT.readActivities(name,events,true);
		List<OD> ods = null;
		return new Instance_TT(name,globalPeriod,transferPenalty,events,activities,ods,0);
	}

	private static List<OD> readODs(String name) throws IOException {
		String prefix = "data/"+name+"/";
		List<OD> ods = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(prefix+"OD.csv"));
		String line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		while (line != null)
		{
			String[] data = line.split("; ");
			int origin = Integer.parseInt(data[0]);
			int destination = Integer.parseInt(data[1]);
			int passengers = Integer.parseInt(data[2]);
			
			ods.add(new OD(origin,destination,passengers));
			// Read next line.
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return ods;
	}

	private static List<Activity_TT> readActivities(String name, List<Event_TT> events, boolean ePESP) throws IOException {
		String prefix = "data/"+name+"/";
		List<Activity_TT> activities = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(prefix+"Activities.csv"));
		String line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		while (line != null)
		{
			String[] data = line.split("; ");
			int id = Integer.parseInt(data[0]);
			String type = data[1].substring(1, data[1].length()-1);
			int fromID = Integer.parseInt(data[2]);
			int toID =  Integer.parseInt(data[3]);
			int lb =  Integer.parseInt(data[4]);
			int ub =  Integer.parseInt(data[5]);
			double weight = 0;
			if(ePESP) {
				weight = Double.parseDouble(data[6]);
			}
			Event_TT from = events.get(fromID-1);
			Event_TT to = events.get(toID-1);
			Activity_TT act = new Activity_TT(id, from, to, lb, ub, weight, type);
			if(!type.equals("turnaround")) {
				activities.add(act);
				if(act.getType().equals("drive")||act.getType().equals("wait")) {
					from.setSuccessor(act);
					to.setPredecessor(act);
				}
			} else {
				System.out.println("Ignoring turnaround");
			}

			// Read next line.
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return activities;
	}

	private static List<Event_TT> readEvents(String name, int globalPeriod, boolean ePESP) throws IOException {
		String prefix = "data/"+name+"/";
		List<Event_TT> events = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(prefix+"Events.csv"));
		String line = bufferedReader.readLine();
		line = bufferedReader.readLine();
		while (line != null)
		{
			String[] data = line.split("; ");
			int id = Integer.parseInt(data[0]);
			String type = data[1].substring(1, data[1].length()-1);
			int stop_id =  Integer.parseInt(data[2]);
			int line_id =  Integer.parseInt(data[3]);
			String line_direction = data[4];
			int line_freq_repetition =  Integer.parseInt(data[5]);
			int period = globalPeriod; 
			if(ePESP) {
				period = line_freq_repetition;
				line_freq_repetition = 1;
			}
			Event_TT e = new Event_TT(id,type,stop_id,line_id,line_direction,line_freq_repetition,period);
			events.add(e);
			// Read next line.
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return events;
	}


	public int getTotTransferPenalty() {
		return totTransferPenalty;
	}


	public boolean isIrrelevant(Activity a) {
		if(irrelevantActivities==null) {
			return false;
		}
		return irrelevantActivities.contains(a);
	}


	public double getConstantTerm() {
		return constantTerm;
	}
	
	
}
