package org.mappush.model;

public class Bounds {

	private double southLat;
	private double northLat;
	private double westLng;
	private double eastLng;

	public Bounds() {}

	public Bounds(String fromHeader) {
		String[] coordinates = fromHeader.split(",");
		this.southLat = Double.parseDouble(coordinates[0]);
		this.northLat = Double.parseDouble(coordinates[1]);
		this.westLng = Double.parseDouble(coordinates[2]);
		this.eastLng = Double.parseDouble(coordinates[3]);
	}

	public Bounds(double southLat, double northLat, double westLng, double eastLng) {
		this.southLat = southLat;
		this.northLat = northLat;
		this.westLng = westLng;
		this.eastLng = eastLng;
	}

	public double getSouthLat() {
		return southLat;
	}

	public void setSouthLat(double southLat) {
		this.southLat = southLat;
	}

	public double getNorthLat() {
		return northLat;
	}

	public void setNorthLat(double northLat) {
		this.northLat = northLat;
	}

	public double getWestLng() {
		return westLng;
	}

	public void setWestLng(double westLng) {
		this.westLng = westLng;
	}

	public double getEastLng() {
		return eastLng;
	}

	public void setEastLng(double eastLng) {
		this.eastLng = eastLng;
	}

	public boolean contains(double lat, double lng) {
		if (lat > southLat && lat <= northLat && lng > westLng && lng <= eastLng) return true;
		else return false;
	}

	public boolean contains(Event event) {
		return contains(event.getLat(), event.getLng());
	}

	@Override
	public String toString() {
		return "Bounds [southLat=" + southLat + ", northLat=" + northLat + ", westLng=" + westLng + ", eastLng=" + eastLng + "]";
	}

}
