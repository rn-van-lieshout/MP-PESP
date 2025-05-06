package pesp;

import java.util.Objects;

import digraph.DirectedGraphNodeIndex;

public class Event extends DirectedGraphNodeIndex {
	protected final int period; //the event occurs every interval time periods
	protected final int id;
	
	public Event(int id, int interval) {
		super();
		this.period = interval;
		this.id = id;
	}
	
	

	public int getId() {
		return id;
	}



	public int getPeriod() {
		return period;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, period);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		return id == other.id && period == other.period;
	}
	
	
	
}
