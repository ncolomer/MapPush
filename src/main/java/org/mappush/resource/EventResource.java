package org.mappush.resource;

import java.util.Random;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
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
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.jersey.Broadcastable;
import org.atmosphere.jersey.SuspendResponse;
import org.mappush.atmosphere.EventListenerImpl;
import org.mappush.filter.BoundsFilter;
import org.mappush.model.Bounds;
import org.mappush.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * curl -v -N -X GET http://localhost:8080/MapPush/api
 * boundsHeader='48.920364,48.922168,2.498703,2.500076'
 * boundsHeader='38.920364,38.922168,12.498703,12.500076'
 * curl -v -N -X GET http://localhost:8080/MapPush/api -H "X-MAP-BOUNDS: $boundsHeader"
 * </pre>
 * 
 * <pre>
 * event='{"lat":48.921266,"lng":2.499390,"pow":1}'
 * curl -v -X POST http://localhost:8080/MapPush/api/event -d $event -H "Content-Type: application/json"
 * </pre>
 * 
 * <pre>
 * bounds='{"infLat":48.920364,"supLat":48.922168,"infLng":2.498703,"supLng":2.500076}'
 * curl -v -X POST http://localhost:8080/MapPush/api/update -d $bounds -H "Content-Type: application/json"
 * </pre>
 * 
 * @author Nicolas
 *
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

	private static final Logger logger = LoggerFactory.getLogger(EventResource.class);
	private static final Random random = new Random();
	private static Thread generator;
	
	private @Context BroadcasterFactory bf;

	private Broadcaster getBroadcaster() {
        return bf.lookup(DefaultBroadcaster.class, "MapPush", true);
    }

	@PostConstruct
	public void init() throws Exception {
		logger.info("Initializing ApiResource");
		BroadcasterConfig config = getBroadcaster().getBroadcasterConfig();
		config.addFilter(new BoundsFilter());
	}

	@GET
	public SuspendResponse<String> connect(@Context HttpServletRequest req,
			@HeaderParam("X-Map-Bounds") Bounds bounds) {
		if (bounds != null) req.getSession(true).setAttribute("bounds", bounds);
		return new SuspendResponse.SuspendResponseBuilder<String>()
				.broadcaster(getBroadcaster())
				.outputComments(true)
				.addListener(new EventListenerImpl() {

					@Override
					public void onMessage(WebSocketEvent event) {
						Bounds bounds = JsonUtils.fromJson(event.message(), Bounds.class);
						if (bounds == null) return;
						logger.info("New bounds {} for resource {}", event.message(), event.webSocket().resource().hashCode());
						HttpServletRequest req = (HttpServletRequest) event.webSocket().resource().getRequest();
						req.getSession(true).setAttribute("bounds", bounds);
					}
					
				})
				.build();
	}

	@POST
	@Path("/event")
	@Consumes(MediaType.APPLICATION_JSON)
	@Broadcast
	public Broadcastable event(Event event) {
		logger.info("/event triggered: {}", event);
		return new Broadcastable(event, "", getBroadcaster());
	}
	
	@GET
	@Path("/start")
	public Response start() {
		if (generator == null) {
			logger.info("Starting event generator");
			generator = new Thread(new Runnable() {
				
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

			}, "Event generator");
			generator.start();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("/stop")
	public Response stop() {
		if (generator != null) {
			logger.info("Stoping event generator");
			generator.interrupt();
			generator = null;
		}
		return Response.ok().build();
	}

}
