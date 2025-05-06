package pesp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import digraph.DirectedGraphArc;
import util.Arithmetic;


public class Cycle
{
	private List<Activity> activities;
	private Set<Activity> forward;
	private Set<Activity> backward;
	private int gcd;
	
	// bounds on the value of the q-variable of this cycle
	private int lower;
	private int upper;
	private Activity generator;

	public Cycle(List<DirectedGraphArc<Event,Activity>> activitiesDAG)
	{
		this.activities = new ArrayList<>();
		for(DirectedGraphArc<Event,Activity> arc: activitiesDAG) {
			activities.add(arc.getData());
		}
		forward = new HashSet<>();
		backward = new HashSet<>();
		// check the forward and backward arcs
		Event curE = activities.get(0).getFrom();
		gcd = curE.getPeriod();
		for (Activity a : activities)
		{
			if (a.getFrom().equals(curE))
			{
				forward.add(a);
				curE = a.getTo();
				gcd = Arithmetic.gcd(gcd,curE.getPeriod());
			}
			else
			{
				backward.add(a);
				curE = a.getFrom();
				gcd = Arithmetic.gcd(gcd,curE.getPeriod());
			}
		}
		if (!curE.equals(activities.get(0).getFrom()))
		{
			throw new Error("Not a cycle");
		}
		computeLowerAndUpper();
	}

	/**
	 * Method to compute the lower and upper bounds on the cycle variable
	 */
	private void computeLowerAndUpper()
	{
		double sumLower = 0;
		double sumUpper = 0;
		for (Activity a : forward)
		{
			sumLower = sumLower + a.getLb();
			sumUpper = sumUpper + a.getUb();
		}
		for (Activity a : backward)
		{
			sumLower = sumLower - a.getUb();
			sumUpper = sumUpper - a.getLb();
		}
		lower = (int) Math.ceil(sumLower / (double) gcd);
		upper = (int) Math.floor(sumUpper / (double) gcd);
	}

	public List<Activity> getActivities()
	{
		return activities;
	}

	public Set<Activity> getForward()
	{
		return forward;
	}

	public Set<Activity> getBackward()
	{
		return backward;
	}

	public int getLower()
	{
		return lower;
	}

	public int getUpper()
	{
		return upper;
	}

	public Activity getGenerator()
	{
		return generator;
	}

	public void setGenerator(Activity generator)
	{
		this.generator = generator;
	}
	
	

	public int getGcd() {
		return gcd;
	}

	@Override
	public String toString()
	{
		return "Cycle [lower=" + lower + ", upper=" + upper + " acts=" + activities + "]";
	}
}
