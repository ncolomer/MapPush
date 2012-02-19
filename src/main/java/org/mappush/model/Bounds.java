package org.mappush.model;

public class Bounds {

	private double northLat;
	private double southLat;
	private double westLng;
	private double eastLng;
	
	public Bounds() {}

	public Bounds(String fromHeader) {
		String[] coordinates = fromHeader.split(",");
		this.northLat = Double.parseDouble(coordinates[0]);
		this.southLat = Double.parseDouble(coordinates[1]);
		this.westLng = Double.parseDouble(coordinates[2]);
		this.eastLng = Double.parseDouble(coordinates[3]);
	}
	
	public Bounds(double northLat, double southLat, double westLng, double eastLng) {
		this.northLat = northLat;
		this.southLat = southLat;
		this.westLng = westLng;
		this.eastLng = eastLng;
	}
	
	public double getNorthLat() {
		return northLat;
	}
	public void setNorthLat(double northLat) {
		this.northLat = northLat;
	}
	public double getSouthLat() {
		return southLat;
	}
	public void setSouthLat(double southLat) {
		this.southLat = southLat;
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
	
	public final boolean contains(double lat, double lng) {
		if (lat <= northLat && lat > southLat && lng <= eastLng && lng > westLng) return true;
		else return false;
	}
	
	public boolean contains(Event nuke) {
		return contains(nuke.getLat(), nuke.getLng());
	}

	@Override
	public String toString() {
		return "Bounds[" +
				"northLat=" + northLat + ", " +
				"southLat=" + southLat + ", " +
				"westLng=" + westLng + ", " +
				"eastLng=" + eastLng +
				"]";
	}

}
