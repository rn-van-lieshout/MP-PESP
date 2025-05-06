package pesp;

import util.Arithmetic;

public class Activity {
	private final int id;
	private final Event from;
	private final Event to;
	private final int lb;
	private final int ub;
	private double weight;
	
	public Activity(int id, Event from, Event to, int lb, int ub, double weight) {
		super();
		this.id = id;
		this.from = from;
		this.to = to;
		this.lb = lb;
		this.ub = ub;
		this.weight = weight;
	}

	public int getID() {
		return id;
	}
	
	public Event getFrom() {
		return from;
	}

	public Event getTo() {
		return to;
	}

	public int getLb() {
		return lb;
	}

	public int getUb() {
		return ub;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	public int getSpan() {
		return ub-lb;
	}
	
	public boolean isFeasible(int x, int pi_i, int pi_j) {
		if(x<lb||x>ub) {
			System.out.println("outside bounds");
			return false;
		}
		int T = getPeriodicity();
		int diff = pi_j-pi_i;
		if(Arithmetic.mod(x, T)!=Arithmetic.mod(diff, T)) {
			//System.out.println("Periodicity "+T+" x mod T: "+Arithmetic.mod(x, T) + " diff mod T "+Arithmetic.mod(diff, T));
			return false;
		}
		
		return true;
	}

	public int getPeriodicity() {
		return Arithmetic.gcd(from.getPeriod(), to.getPeriod());
	}

	@Override
	public String toString() {
		return "Activity [from=" + from + ", to=" + to + ", lb=" + lb + ", ub=" + ub + ", weight=" + weight + "]";
	}


	
}
