package instance;

import pesp.Activity;

public class Activity_TT extends Activity {

	private final String type;

	public Activity_TT(int id, Event_TT from, Event_TT to, int lb, int ub, double weight, String type) {
		super(id,from, to, lb, ub, weight);
		// TODO Auto-generated constructor stub
		this.type = type;
	}
	
	public Event_TT getFrom() {
		return (Event_TT) super.getFrom();
	}
	
	public Event_TT getTo() {
		return (Event_TT) super.getTo();
	}

	public String getType() {
		return type;
	}

	public void incrementWeight(double d) {
		setWeight(getWeight()+d);
		
	}
	
	

	
}
