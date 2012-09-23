package org.mappush.resource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketResourceTest extends BaseTest {

	@Test(enabled = true)
	public void sendPing() throws Exception {
		// setup
		final String ping = "PING";
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<String> receivedEvent = new AtomicReference<String>();
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().build();
		WebSocket webSocket = client.prepareGet(getWsUrl("")).execute(handler).get();
		assertNotNull(webSocket);
		assertEquals(webSocket.isOpen(), true);
		webSocket.addWebSocketListener(new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.set(message);
				latch.countDown();
			}
		});
		// test
		webSocket.sendPing(ping.getBytes());
		// assert
		assertEquals(latch.await(10, TimeUnit.SECONDS), true);
		assertEquals(receivedEvent.get(), ping);
		webSocket.close();
	}

	@Test(enabled = true)
	public void sendMessage_withNoBounds_shouldBroadcast() throws Exception {
		// setup
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
		// test
		client.preparePost(getHttpUrl("event")).setBody(sentEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(onMessageLatch.await(10, TimeUnit.SECONDS), true);
		assertEquals(receivedEvent.get(), sentEvent);
		webSocket.close();
	}

	@Test(enabled = true)
	public void sendMessage_withBoundsHeader_shouldBroadcast() throws Exception {
		// setup
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
		// test
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(getHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(latch.await(10, TimeUnit.SECONDS), true);
		assertEquals(counter.get(), 1);
		webSocket1.close();
		webSocket2.close();
	}

	@Test(enabled = true)
	public void sendMessage_withBoundsChange_shouldBroadcast() throws Exception {
		// setup
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
		// test
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		webSocket1.sendTextMessage(clientBounds);
		client.preparePost(getHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(getHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(latch.await(10, TimeUnit.SECONDS), true);
		assertEquals(counter.get(), 2);
		webSocket1.close();
		webSocket2.close();
	}

}
