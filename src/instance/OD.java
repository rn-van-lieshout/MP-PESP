package instance;

import java.util.Objects;

public class OD {
	private final int origin;
	private final int destination;
	private final int passengers;
	
	public OD(int origin, int destination, int passengers) {
		this.origin = origin;
		this.destination = destination;
		this.passengers = passengers;
	}

	public int getOrigin() {
		return origin;
	}

	public int getDestination() {
		return destination;
	}

	public int getPassengers() {
		return passengers;
	}

	@Override
	public int hashCode() {
		return Objects.hash(destination, origin, passengers);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OD other = (OD) obj;
		return destination == other.destination && origin == other.origin && passengers == other.passengers;
	}
	
	
}
