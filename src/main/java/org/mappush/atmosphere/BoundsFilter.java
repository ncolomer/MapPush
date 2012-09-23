package org.mappush.atmosphere;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.mappush.model.Bounds;
import org.mappush.model.Event;
import org.mappush.resource.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoundsFilter implements PerRequestBroadcastFilter {

	private final Logger LOG = LoggerFactory.getLogger(BoundsFilter.class);

	@Override
	public BroadcastAction filter(Object originalMessage, Object message) {
		return new BroadcastAction(ACTION.CONTINUE, originalMessage);
	}

	@Override
	public BroadcastAction filter(AtmosphereResource resource, Object originalMessage, Object message) {
		LOG.info("BoundsFilter triggered for resource {} with message {}", resource.hashCode(), message);
		Event event = (Event) message;
		Bounds bounds = (Bounds) resource.getRequest().getAttribute("bounds");
		if (bounds != null) {
			if (bounds.contains(event)) {
				LOG.debug("Bounds matched: applying action CONTINUE");
				String json = JsonUtils.toJson(event);
				return new BroadcastAction(ACTION.CONTINUE, json);
			} else {
				LOG.debug("Bounds not matched: applying action ABORT");
				return new BroadcastAction(ACTION.ABORT, message);
			}
		} else {
			LOG.debug("No bounds: applying default action CONTINUE");
			String json = JsonUtils.toJson(event);
			return new BroadcastAction(ACTION.CONTINUE, json);
		}
	}

}