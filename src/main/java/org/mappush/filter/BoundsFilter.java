package org.mappush.filter;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.mappush.model.Bounds;
import org.mappush.model.Event;
import org.mappush.resource.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoundsFilter implements PerRequestBroadcastFilter {

	private static final Logger logger = LoggerFactory.getLogger(BoundsFilter.class);

	@Override
	public BroadcastAction filter(Object originalMessage, Object message) {
		return new BroadcastAction(ACTION.CONTINUE, originalMessage);
	}
	
	@Override
	public BroadcastAction filter(AtmosphereResource res, Object originalMessage, Object message) {
		AtmosphereRequest request = res.getRequest();
		logger.info("BoundsFilter triggered for AtmosphereResource {} with message {}", res.hashCode(), message);
		Event event = (Event) message;
		try {
			Bounds bounds = (Bounds) request.getAttribute("bounds");
			if (bounds == null) throw new NoBoundsException("no bounds");
			if (bounds.contains(event)) {
				String json = JsonUtils.toJson(event); // Manual serialization
				return new BroadcastAction(ACTION.CONTINUE, json);
			} else {
				return new BroadcastAction(ACTION.ABORT, message);
			}
		} catch (NoBoundsException e) {
			logger.info("Applying default action CONTINUE, cause: {}", e.getMessage());
			String json = JsonUtils.toJson(event); // Manual serialization
			return new BroadcastAction(ACTION.CONTINUE, json);
		} catch (Exception e) {
			logger.info("Filter BoundsFilter aborted, cause: {}", e.getMessage());
			return new BroadcastAction(ACTION.ABORT, message);
		}
	}

	public class NoBoundsException extends Exception {

		private static final long serialVersionUID = -1479015550025668286L;
		
		public NoBoundsException(String string) {
			super(string);
		}

	}
	
}