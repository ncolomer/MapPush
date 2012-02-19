package org.mappush.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
	public BroadcastAction filter(HttpServletRequest request, HttpServletResponse response, Object message) {
		logger.info("BoundsFilter triggered for client {} with message {}", request.getLocalAddr(), message);
		Event nuke = (Event) message;
		try {
			HttpSession session = request.getSession(false);
			if (session == null) throw new NoBoundsException();
			Bounds bounds = (Bounds) session.getAttribute("bounds");
			if (bounds == null) throw new NoBoundsException();
			if (bounds.contains(nuke)) {
				String json = JsonUtils.toJson(nuke);
				return new BroadcastAction(ACTION.CONTINUE, json);
			} else {
				return new BroadcastAction(ACTION.ABORT, message);
			}
		} catch (NoBoundsException e) {
			logger.info("Filter BoundsFilter aborted, cause: {}", e.getMessage());
			return new BroadcastAction(ACTION.ABORT, message);
		} catch (Exception e) {
			logger.info("Filter BoundsFilter aborted, cause: {}", e.getMessage());
			return new BroadcastAction(ACTION.ABORT, message);
		}
	}
	
	public class NoBoundsException extends Exception {
		private static final long serialVersionUID = -1479015550025668286L;
	}

}
