package org.mappush.resource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketResourceTest extends BaseTest {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketResourceTest.class);

	@Test(enabled = true)
	public void sendPing() throws Exception {
		logger.info("{}: running testWebSocketConnection", getClass().getSimpleName());
		final String ping = "PING";
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<String> receivedEvent = new AtomicReference<String>();
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().build();
		WebSocket webSocket = client.prepareGet(getWsUrl("")).execute(handler).get();
		assertNotNull(webSocket, "WebSocket connection should be null");
		assertEquals(webSocket.isOpen(), true, "WebSocket connection should be open");
		webSocket.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.set(message);
				latch.countDown();
			}
		});
		webSocket.sendPing(ping.getBytes());
		assertEquals(latch.await(10, TimeUnit.SECONDS), true, "The ping message should have been returned");
		assertEquals(receivedEvent.get(), ping, "The ping message is not the same");
		webSocket.close();
	}

	@Test(enabled = true)
	public void sendMessage_withNoBounds_shouldBroadcast() throws Exception {
		logger.info("{}: running testWebSocketBroadcastMessageNoBounds", getClass().getSimpleName());
		final String sentEvent = "{\"lat\":0.0,\"lng\":0.0}";
		final CountDownLatch onMessageLatch = new CountDownLatch(1);
		final AtomicReference<String> receivedEvent = new AtomicReference<String>();
		WebSocket webSocket = getWebSocket();
		webSocket.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.set(message);
				onMessageLatch.countDown();
			}
		});
		client.preparePost(getHttpUrl("event")).setBody(sentEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertEquals(onMessageLatch.await(10, TimeUnit.SECONDS), true, "No message was broadcasted by the server");
		assertEquals(receivedEvent.get(), sentEvent, "The received event is not the same as the one sent");
		webSocket.close();
	}

	@Test(enabled = true)
	public void sendMessage_withBoundsHeader_shouldBroadcast() throws Exception {
		logger.info("{}: running testWebSocketBroadcastMessageWithBoundsHeader", getClass().getSimpleName());
		final String clientBounds = "4.0,6.0,4.0,6.0";
		final String inBoundsEvent = "{\"lat\":5.0,\"lng\":5.0}";
		final String outBoundsEvent = "{\"lat\":10.0,\"lng\":10.0}";
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicInteger counter = new AtomicInteger(0);
		BoundRequestBuilder builder = client.prepareGet(getWsUrl("")).addHeader("X-Map-Bounds", clientBounds);
		WebSocket webSocket1 = getWebSocket(builder);
		webSocket1.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				counter.incrementAndGet();
			}
		});
		WebSocket webSocket2 = getWebSocket();
		webSocket2.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(getHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertEquals(latch.await(10, TimeUnit.SECONDS), true, "Not enough message was broadcasted by the server");
		assertEquals(counter.get(), 1, "The client should have received exactly one event");
		webSocket1.close();
		webSocket2.close();
	}

	@Test(enabled = true)
	public void sendMessage_withBoundsChange_shouldBroadcast() throws Exception {
		logger.info("{}: running testWebSocketBroadcastMessageWithBoundsChange", getClass().getSimpleName());
		final String clientBounds = "{\"southLat\":4.0,\"northLat\":6.0,\"westLng\":4.0,\"eastLng\":6.0}";
		final String inBoundsEvent = "{\"lat\":5.0,\"lng\":5.0}";
		final String outBoundsEvent = "{\"lat\":10.0,\"lng\":10.0}";
		final CountDownLatch latch = new CountDownLatch(3);
		final AtomicInteger counter = new AtomicInteger(0);
		WebSocket webSocket1 = getWebSocket();
		webSocket1.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				counter.incrementAndGet();
			}
		});
		WebSocket webSocket2 = getWebSocket();
		webSocket2.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		webSocket1.sendTextMessage(clientBounds);
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(getHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertEquals(latch.await(10, TimeUnit.SECONDS), true, "Not enough message was broadcasted by the server");
		assertEquals(counter.get(), 2, "The client should have received exactly two events");
		webSocket1.close();
		webSocket2.close();
	}

}
