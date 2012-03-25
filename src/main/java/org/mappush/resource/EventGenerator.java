package org.mappush.resource;

import java.util.Random;

import org.atmosphere.cpr.Broadcaster;
import org.mappush.model.Event;

public class EventGenerator {
	
	private final Random random = new Random();
	
	private Broadcaster broadcaster;
	private int interval;
	private Thread generator;
	
	public EventGenerator(Broadcaster broadcaster, int interval) {
		this.broadcaster = broadcaster;
		this.interval = interval;
	}
	
	public void start() {
		if (generator == null) {
			generator = new Thread(new Generator() , "GeneratorThread");
			generator.start();
		}
	}
	
	public void stop() {
		if (generator != null) {
			generator.interrupt();
			generator = null;
		}
	}
	
	public class Generator implements Runnable {
		
		@Override
		public void run() {
			while(true) {
				try {
					double lat = (random.nextInt((int)(180*10E6)) - 90*10E6) / 10E6 ;
					double lng = (random.nextInt((int)(360*10E6)) - 180*10E6) / 10E6 ;
					broadcaster.broadcast(new Event(lat, lng));
					Thread.sleep(interval);
				} catch (InterruptedException e) {break;}
			}
		}

	}

}
