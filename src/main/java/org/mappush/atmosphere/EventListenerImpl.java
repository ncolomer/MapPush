package org.mappush.atmosphere;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListener;

public class EventListenerImpl implements WebSocketEventListener {

	public void onSuspend(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {}

	public void onResume(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {}

	public void onDisconnect(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {}

	public void onBroadcast(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {}

	public void onThrowable(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {}

	public void onHandshake(WebSocketEvent event) {}

	public void onMessage(WebSocketEvent event) {}

	public void onClose(WebSocketEvent event) {}

	public void onControl(WebSocketEvent event) {}

	public void onDisconnect(WebSocketEvent event) {}

	public void onConnect(WebSocketEvent event) {}

}
