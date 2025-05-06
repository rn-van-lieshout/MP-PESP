package instance;

import java.util.Objects;

import pesp.Event;

public class Event_TT extends Event {

	private final String type;
	private final int stop_id;
	private final int line_id;
	private final String line_direction;
	private final int line_freq_repetition;
	
	private Activity_TT predecessor; //previous activity, only considering the drive and wait
	private Activity_TT successor; //next activity
	
	public Event_TT(int id, String type, int stop_id, int line_id, String line_direction, int line_freq_repetition, int period) {		
		super(id, period);
		// TODO Auto-generated constructor stub
		this.type = type;
		this.stop_id = stop_id;
		this.line_id = line_id;
		this.line_direction = line_direction;
		this.line_freq_repetition = line_freq_repetition;
	}
	
	public String getType() {
		return type;
	}

	public int getStop_id() {
		return stop_id;
	}

	public int getLine_id() {
		return line_id;
	}

	public String getLine_direction() {
		return line_direction;
	}

	public int getLine_freq_repetition() {
		return line_freq_repetition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(line_direction, line_freq_repetition, line_id, stop_id, type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event_TT other = (Event_TT) obj;
		return Objects.equals(line_direction, other.line_direction)
				&& line_freq_repetition == other.line_freq_repetition && line_id == other.line_id
				&& stop_id == other.stop_id && Objects.equals(type, other.type);
	}

	public boolean sameEventExceptRepetition(Event_TT other) {
		if(!type.equals(other.type)) {
			return false;
		}
		if(this.stop_id!=other.stop_id) {
			return false;
		}
		if(this.line_id!=other.line_id) {
			return false;
		}
		if(!line_direction.equals(other.line_direction)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Event_TT [id=" + id +", type=" + type + ", stop_id=" + stop_id + ", line_id=" + line_id + ", line_direction="
				+ line_direction + ", line_freq_repetition=" + line_freq_repetition + ", period="+period+"]";
	}

	public Activity_TT getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Activity_TT predecessor) {
		this.predecessor = predecessor;
	}

	public Activity_TT getSuccessor() {
		return successor;
	}

	public void setSuccessor(Activity_TT successor) {
		this.successor = successor;
	}
	
	
	

}
