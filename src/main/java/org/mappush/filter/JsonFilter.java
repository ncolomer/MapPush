package org.mappush.filter;

import org.atmosphere.cpr.BroadcastFilter;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.mappush.model.Event;
import org.mappush.resource.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonFilter implements BroadcastFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(JsonFilter.class);

	@Override
	public BroadcastAction filter(Object originalMessage, Object message) {
		logger.info("Filter JsonFilter for message: {}", message);
		try {
			Event event = (Event) message;
			String json = JsonUtils.toJson(event);
			return new BroadcastAction(ACTION.CONTINUE, json);
		} catch (Exception e) {
			logger.info("Filter aborted for message [{}], cause: {}", message, e.getMessage());
			return new BroadcastAction(ACTION.ABORT, message);
		}
	}

}
