package org.mappush.model;

public class Event {
	
	private double lat;
	private double lng;
	
	public Event() {}
	
	public Event(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}

	@Override
	public String toString() {
		return "Event[" +
				"lat=" + lat + ", " +
				"lng=" + lng + ", " +
				"]";
	}

}
