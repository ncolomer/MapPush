package org.mappush.atmosphere;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.mappush.model.Bounds;
import org.mappush.resource.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener extends WebSocketEventListenerAdapter {
	
	private final Logger logger = LoggerFactory.getLogger(EventListener.class);

	@Override
	public void onMessage(WebSocketEvent event) {
		logger.info("WebSocket message received from client");
		Bounds bounds = JsonUtils.fromJson(event.message(), Bounds.class);
		if (bounds == null) return;
		logger.info("New bounds {} for resource {}", event.message(), event.webSocket().resource().hashCode());
		AtmosphereRequest req = event.webSocket().resource().getRequest();
		req.setAttribute("bounds", bounds);
	}

}
