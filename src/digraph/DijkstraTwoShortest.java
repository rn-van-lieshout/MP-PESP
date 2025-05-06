package digraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


public class DijkstraTwoShortest<V extends DirectedGraphNodeIndex, A> {
	private final DirectedGraph<V, A> dag;
	private final int maxLabels;
	private Map<V,List<Label<V,A>>> labelsPerNode;
	private final V origin;

		
	public DijkstraTwoShortest(DirectedGraph<V, A> dag, V origin, int maxLabels) {
		this.dag = dag;
		this.maxLabels = maxLabels;
		labelsPerNode = new LinkedHashMap<>();
		this.origin = origin;
	}
	
	public double getDistance(V n) {
		List<Label<V,A>> labels = labelsPerNode.get(n);
		Collections.sort(labels);
		return labels.get(0).getCost();
	}
	
	public List<List<DirectedGraphArc<V, A>>> getPaths(V to) {
		List<Label<V,A>> labels = labelsPerNode.get(to);
		List<List<DirectedGraphArc<V, A>>> bestRoutes = new ArrayList<>();
		Collections.sort(labels);//, Comparator.comparingDouble(item -> item.getCost())); 
		//System.out.println("NR of labels at to "+labels.size());
		if(labels.size()>2) {
			throw new Error("To: "+to);
		}
		for(Label<V,A> label: labels) {
			List<DirectedGraphArc<V, A>> arcList = new ArrayList<>();
			Label<V,A> curLabel = label;
			while(curLabel.getPredecessor()!=null) {
				arcList.add(curLabel.getLastArc());
				curLabel = curLabel.getPredecessor();
			}
			Collections.reverse(arcList);
			bestRoutes.add(arcList);
		}
		return bestRoutes;
	}

	
	public void computeDistances() {
		//initialise the labels
		for(V node: dag.getNodes()) {
			labelsPerNode.put(node,new ArrayList<>());
		}
		//add the label for the origin
		labelsPerNode.get(origin).add(new Label<V,A>(origin,0,null,null));
		
		List<Label<V,A>> toScan = new ArrayList<>();
		toScan.add(labelsPerNode.get(origin).get(0));

		//System.out.println("Start scanning");
		while(!toScan.isEmpty()) {
			Label<V,A> label = toScan.get(0);
			List<Label<V,A>> newLabels = scan(label);
			toScan.remove(0);
			toScan.addAll(newLabels);
		}
		//System.out.println("Stop scanning");
	}
	
	private List<Label<V, A>> scan(Label<V,A> label) {
		//System.out.println("Scanning: "+label);
		List<Label<V, A>> newLabels = new ArrayList<>();
		double oldCost = label.getCost();
		for(DirectedGraphArc<V, A> outArc: dag.getOutArcs(label.getNode())) {
			double cost = outArc.getWeight();
			double newCost = oldCost+cost;
			Label<V,A> newLabel = new Label<>(outArc.getTo(),newCost,label,outArc);
			boolean added = addLabel(newLabel);
			if(added) {
				//System.out.println("New label "+label);
				newLabels.add(newLabel);
			}
		}
		return newLabels;
	}
	
	/**
	 * Method that adds the label after a dominance check
	 */
	private boolean addLabel(Label<V,A> newLabel) {
		List<Label<V,A>> existingLabels = labelsPerNode.get(newLabel.getNode());
		if(existingLabels.size()>=maxLabels&&newLabel.getCost()>existingLabels.get(maxLabels-1).getCost()) {
			//longer than 
			return false;
		}
		existingLabels.add(newLabel);
		Collections.sort(existingLabels);
		if(existingLabels.size()>maxLabels) {
			existingLabels.remove(maxLabels);
		}
		if(existingLabels.size()>2) {
			throw new Error("wattt");
		}
		return true;
	}

	private class Label<V extends DirectedGraphNodeIndex, A> implements Comparable<Label<V,A>> {
		private final V node;
		private final double cost;
		private final DirectedGraphArc<V,A> lastArc;
		private final Label<V,A> predecessor;
		
		public Label(V node, double cost, Label<V,A> predecessor, DirectedGraphArc<V,A> lastArc) {
			super();
			this.node = node;
			this.cost = cost;
			this.lastArc = lastArc;
			this.predecessor = predecessor;
		}

		public DirectedGraphArc<V,A> getLastArc() {
			return lastArc;
		}

		public V getNode() {
			return node;
		}

		public double getCost() {
			return cost;
		}

		public Label<V,A> getPredecessor() {
			return predecessor;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(cost, node, predecessor);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Label<V,A> other = (Label) obj;
			return cost == other.cost && Objects.equals(node, other.node)
					&& Objects.equals(predecessor, other.predecessor);
		}

		@Override
		public String toString() {
			return "Label [node=" + node + ", distance=" + cost +"]";
		}
		
		public int compareTo(Label<V,A> o) {
			return Double.compare(this.cost, o.cost);
		}
	}
	
	
	
}


