package org.mappush.resource;

import java.util.Random;

import org.atmosphere.cpr.Broadcaster;
import org.mappush.model.Event;

public class EventGenerator {

	private static final String GENERATOR_THREAD_NAME = "GeneratorThread";

	private volatile Thread generatorThread;

	/**
	 * Starts the generation of random events. Each event is broadcasted using
	 * the provided Broadcaster. This method does nothing if a Generator is
	 * already running.
	 */
	public synchronized void start(Broadcaster broadcaster, int interval) {
		if (generatorThread == null) {
			Generator generator = new Generator(broadcaster, interval);
			generatorThread = new Thread(generator, GENERATOR_THREAD_NAME);
			generatorThread.start();
		}
	}

	/**
	 * Stops the generation of random events. This method does nothing if no
	 * Generator was started before.
	 */
	public synchronized void stop() {
		if (generatorThread != null) {
			Thread tmpReference = generatorThread;
			generatorThread = null;
			tmpReference.interrupt();
		}
	}

	private class Generator implements Runnable {

		private final Random random;
		private final Broadcaster broadcaster;
		private final int interval;

		public Generator(Broadcaster broadcaster, int interval) {
			this.random = new Random();
			this.broadcaster = broadcaster;
			this.interval = interval;
		}

		@Override
		public void run() {
			Thread currentThread = Thread.currentThread();
			while (currentThread == generatorThread) {
				try {
					double lat = random.nextInt((int) 180E6) / 10E6 - 90;
					double lng = random.nextInt((int) 360E6) / 10E6 - 180;
					broadcaster.broadcast(new Event(lat, lng));
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

	}

}
