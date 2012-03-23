package org.mappush.resource;

import java.util.Random;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.atmosphere.annotation.Broadcast;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.jersey.Broadcastable;
import org.atmosphere.jersey.SuspendResponse;
import org.mappush.atmosphere.EventListener;
import org.mappush.filter.BoundsFilter;
import org.mappush.model.Bounds;
import org.mappush.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * curl -v -N -X GET http://localhost:8080/MapPush/api
 * boundsHeader='48.0,49.0,2.0,3.0'
 * curl -v -N -X GET http://localhost:8080/MapPush/api -H "X-MAP-BOUNDS: $boundsHeader"
 * </pre>
 * 
 * <pre>
 * event='{"lat":48.921266,"lng":2.499390}'
 * curl -v -X POST http://localhost:8080/MapPush/api/event -d $event -H "Content-Type: application/json"
 * </pre>
 * 
 * @author Nicolas
 *
 */

@Path("/")
//@Singleton // <== problem ?
public class EventResource {

	private static final Logger logger = LoggerFactory.getLogger(EventResource.class);
	
	private static final Random random = new Random();
	private static final BoundsFilter filter = new BoundsFilter();
	private static final EventListener listener = new EventListener();
	
	private static Thread generator;
	
	private @Context BroadcasterFactory bf;
	
	/**
	 * Programmatic way to get a Broadcaster instance
	 * @return the MapPush Broadcaster
	 */
	private Broadcaster getBroadcaster() {
		return bf.lookup(DefaultBroadcaster.class, "MapPush", true);
    }

	/**
	 * The @PostConstruct annotation makes this method executed by the 
	 * container after this class is instanciated. It is one way to initialize 
	 * the Broadcaster by adding some Filters
	 */
	@PostConstruct
	public void init() {
		logger.info("Initializing EventResource");
		BroadcasterConfig config = getBroadcaster().getBroadcasterConfig();
		config.addFilter(filter);
	}

	/**
	 * When the client connects to this URI, the response is suspended or 
	 * upgraded if it is WebSocket capable. A Broadcaster is affected to 
	 * deliver future messages and manage the communication lifecycle.
	 * @param req the request (injected by the container)
	 * @param bounds the bounds (extracted from header and deserialized)
	 * @return a SuspendResponse
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SuspendResponse<String> connect(@Context AtmosphereResource res,
			@HeaderParam("X-Map-Bounds") Bounds bounds) {
		if (bounds != null) res.getRequest().setAttribute("bounds", bounds);
		return new SuspendResponse.SuspendResponseBuilder<String>()
				.broadcaster(getBroadcaster())
				.outputComments(true)
				.addListener(listener)
				.build();
	}

	/**
	 * This URI allows a client to send a new Event that will be broadcaster 
	 * to all other connected clients.
	 * @param event the Event (deserialized from JSON)
	 * @return a Broadcastable
	 */
	@POST
	@Path("event")
	@Consumes(MediaType.APPLICATION_JSON)
	@Broadcast
	public Broadcastable broadcastEvent(Event event) {
		logger.info("New event: {}", event);
		return new Broadcastable(event, "", getBroadcaster());
	}
	
	@GET
	@Path("start")
	public Response start() {
		if (generator == null) {
			logger.info("Starting EventGenerator");
			generator = new Thread(new EventGenerator() , "EventGenerator");
			generator.start();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("stop")
	public Response stop() {
		if (generator != null) {
			logger.info("Stopping EventGenerator");
			generator.interrupt();
			generator = null;
		}
		return Response.ok().build();
	}
	
	public class EventGenerator implements Runnable {
		
		@Override
		public void run() {
			while(true) {
				try {
					double lat = (random.nextInt((int)(180*10E6)) - 90*10E6) / 10E6 ;
					double lng = (random.nextInt((int)(360*10E6)) - 180*10E6) / 10E6 ;
					getBroadcaster().broadcast(new Event(lat, lng));
					Thread.sleep(100);
				} catch (InterruptedException e) {break;}
			}
		}

	}
	

}
