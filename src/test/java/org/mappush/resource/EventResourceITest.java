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
import com.ning.http.client.Response;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class EventResourceITest extends BaseTest {

	@Test
	public void postEvent() throws Exception {
		// setup
		final String event = "{\"lat\":1.234,\"lng\":1.234}";
		// test
		Response response = client.preparePost(buildHttpUrl("event"))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setBody(event)
				.execute().get();
		// assert
		assertEquals(response.getStatusCode(), 200);
	}

	@Test
	public void postEvent_withInvalidData() throws Exception {
		// setup
		final String event = "{\"latitude\":0.0,\"longitude\":0.0}";
		// test
		Response response = client.preparePost(buildHttpUrl("event"))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setBody(event)
				.execute().get();
		// assert
		assertEquals(response.getStatusCode(), 400);
	}

	@Test(enabled = false)
	public void postEvent_withAsyncDisabled() throws Exception {
		// setup
		final String event = "{\"lat\":1.234,\"lng\":1.234}";
		WebSocket webSocket = getWebSocket();
		// test
		Response response = client.preparePost(buildHttpUrl("event"))
				.addQueryParameter("async", "false")
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setBody(event).execute().get();
		// assert
		assertEquals(response.getStatusCode(), 200);
		assertEquals(response.getResponseBody(), "1");
		webSocket.close();
	}

	@Test
	public void sendPing() throws Exception {
		// setup
		final String ping = "PING";
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<String> receivedEvent = new AtomicReference<String>();
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().build();
		WebSocket webSocket = client.prepareGet(buildWsUrl("")).execute(handler).get();
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
		assertEquals(latch.await(1, TimeUnit.SECONDS), true);
		assertEquals(receivedEvent.get(), ping);
		webSocket.close();
	}

	@Test
	public void sendMessage_withNoBounds_shouldBroadcast() throws Exception {
		// setup
		final String event = "{\"lat\":0.0,\"lng\":0.0}";
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
		client.preparePost(buildHttpUrl("event")).setBody(event)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(onMessageLatch.await(1, TimeUnit.SECONDS), true);
		assertEquals(receivedEvent.get(), event);
		webSocket.close();
	}

	@Test
	public void sendMessage_withBoundsHeader_shouldBroadcast() throws Exception {
		// setup
		final String clientBounds = "4.0,6.0,4.0,6.0";
		final String inBoundsEvent = "{\"lat\":5.0,\"lng\":5.0}";
		final String outBoundsEvent = "{\"lat\":10.0,\"lng\":10.0}";
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicInteger counter = new AtomicInteger(0);
		BoundRequestBuilder builder = client.prepareGet(buildWsUrl("")).addHeader("X-Map-Bounds", clientBounds);
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
		client.preparePost(buildHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(buildHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(latch.await(1, TimeUnit.SECONDS), true);
		assertEquals(counter.get(), 1);
		webSocket1.close();
		webSocket2.close();
	}

	@Test
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
		client.preparePost(buildHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		webSocket1.sendTextMessage(clientBounds);
		client.preparePost(buildHttpUrl("event")).setBody(inBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(buildHttpUrl("event")).setBody(outBoundsEvent)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.execute().get();
		// assert
		assertEquals(latch.await(1, TimeUnit.SECONDS), true);
		assertEquals(counter.get(), 2);
		webSocket1.close();
		webSocket2.close();
	}

}
