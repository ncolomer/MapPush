package org.mappush.resource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MediaType;

import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class EventResourceTest {

	protected static final Logger logger = LoggerFactory.getLogger(EventResourceTest.class);

	private AsyncHttpClient client;
	private Nettosphere server;
	private String host;
	private int port;

	@BeforeClass
	public void setUp() throws IOException {
		// Setup AsyncHttpClient
		AsyncHttpClientConfig ahcConfig = new AsyncHttpClientConfig.Builder().build();
		client = new AsyncHttpClient(ahcConfig);
		// Setup Nettosphere
		host = "127.0.0.1";
		port = TestUtils.findFreePort();
		Config nettosphereConfig = new Config.Builder()
				.host(host)
				.port(port)
				.initParam("com.sun.jersey.config.property.packages", "org.mappush.resource;org.mappush.jersey")
				.initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
				.build();
		server = new Nettosphere.Builder().config(nettosphereConfig).build();
		logger.info("Starting Nettosphere on address {}:{}", host, port);
		server.start();
	}

	@AfterClass
	public void tearDown() {
		client.close();
		server.stop();
	}

	@Test(enabled=true)
	public void testNettosphereJerseyConfig() throws Exception {
		Request req = client.prepareGet(TestUtils.buildHttpUrl(host, port, "initparams")).build();
		Response resp = client.executeRequest(req).get();
		assertNotNull(resp, "Response should not be null");
		assertEquals(200, resp.getStatusCode(), "Response status code should be 200");
		assertEquals(MediaType.APPLICATION_JSON, resp.getContentType(), "Response content type should be application/json");
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<Map<String, String>> type = new TypeReference<Map<String,String>>() {};
		Map<String, String> map = mapper.readValue(resp.getResponseBody(), type);
		assertEquals(map.containsKey("com.sun.jersey.api.json.POJOMappingFeature"), true, "Response should contain POJOMappingFeature key");
		assertEquals(map.get("com.sun.jersey.api.json.POJOMappingFeature"), "true", "POJOMappingFeature key should be set to true");
	}

	@Test(enabled=true)
	public void testPostValidEvent() throws Exception {
		final String sentEvent = "{\"lat\":0.0,\"lng\":0.0}";
		Response resp = client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(sentEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertNotNull(resp, "Response should not be null");
		assertEquals(resp.getStatusCode(), 200, "Response status code should be 200");
	}

	@Test(enabled=true)
	public void testPostNonValidEvent() throws Exception {
		final String sentEvent = "{\"latitude\":0.0,\"longitude\":0.0}";
		Response resp = client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(sentEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertNotNull(resp, "Response should not be null");
		assertEquals(resp.getStatusCode(), 400, "Response status code should be 400");
	}

	@Test(enabled=true)
	public void testWebSocketBroadcastMessageNoBounds() throws Exception {
		final String sentEvent = "{\"lat\":0.0,\"lng\":0.0}";
		final CountDownLatch onMessageLatch = new CountDownLatch(1);
		final AtomicReference<String> receivedEvent = new AtomicReference<String>();
		// Configure and execute WebSocket
		WebSocketListener listener = new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.set(message);
				onMessageLatch.countDown();
			}
		};
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
		WebSocket webSocket = client.prepareGet(TestUtils.buildWsUrl(host, port, "")).execute(handler).get();
		assertNotNull(webSocket, "WebSocket connection should be null");
		assertEquals(webSocket.isOpen(), true, "WebSocket connection should be open");
		Thread.sleep(1000);
		// Send an event
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(sentEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		// Await for the broadcast of the server
		assertEquals(onMessageLatch.await(10, TimeUnit.SECONDS), true, "No message was broadcasted by the server");
		assertEquals(receivedEvent.get(), sentEvent, "The received event is not the same as the one sent");
		webSocket.close();

	}

	@Test(enabled=true)
	public void testWebSocketBroadcastMessageWithBoundsHeader() throws Exception {
		final String clientBounds = "4.0,6.0,4.0,6.0";
		final String inBoundsEvent = "{\"lat\":5.0,\"lng\":5.0}";
		final String outBoundsEvent = "{\"lat\":10.0,\"lng\":10.0}";
		final CountDownLatch onMessageLatch = new CountDownLatch(1);
		final AtomicInteger receivedEvent = new AtomicInteger(0);
		// Configure and execute WebSocket
		WebSocketListener listener = new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.incrementAndGet();
				onMessageLatch.countDown();
			}
		};
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
		WebSocket webSocket = client.prepareGet(TestUtils.buildWsUrl(host, port, ""))
				.addHeader("X-Map-Bounds", clientBounds).execute(handler).get();
		assertNotNull(webSocket, "WebSocket connection should be null");
		assertEquals(webSocket.isOpen(), true, "WebSocket connection should be open");
		Thread.sleep(1000);
		// Send events
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(inBoundsEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(outBoundsEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		// Await for the broadcast of the server
		assertEquals(onMessageLatch.await(10, TimeUnit.SECONDS), true, "No message was broadcasted by the server");
		assertEquals(receivedEvent.get(), 1, "The client should have received one event");
		webSocket.close();
	}

	@Test(enabled=true)
	public void testWebSocketBroadcastMessageWithBoundsChange() throws Exception {
		final String clientBounds = "{\"southLat\":4.0,\"northLat\":6.0,\"westLng\":4.0,\"eastLng\":6.0}";
		final String initialEvent = "{\"lat\":0.0,\"lng\":0.0}";
		final String inBoundsEvent = "{\"lat\":5.0,\"lng\":5.0}";
		final String outBoundsEvent = "{\"lat\":10.0,\"lng\":10.0}";
		final CountDownLatch onMessageLatch = new CountDownLatch(2);
		final AtomicInteger receivedEvent = new AtomicInteger(0);
		// Configure and execute WebSocket
		WebSocketListener listener = new WebSocketListenerImpl() {
			@Override
			public void onMessage(String message) {
				receivedEvent.incrementAndGet();
				onMessageLatch.countDown();
			}
		};
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
		WebSocket webSocket = client.prepareGet(TestUtils.buildWsUrl(host, port, "")).execute(handler).get();
		assertNotNull(webSocket, "WebSocket connection should be null");
		assertEquals(webSocket.isOpen(), true, "WebSocket connection should be open");
		Thread.sleep(1000);
		// Send initial event
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(initialEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		// Change bounds and send two events
		webSocket.sendTextMessage(clientBounds);
		//webSocket.sendMessage(clientBounds.getBytes());
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(inBoundsEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		client.preparePost(TestUtils.buildHttpUrl(host, port, "event"))
				.setBody(outBoundsEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		// Await for the broadcast of the server
		assertEquals(onMessageLatch.await(10, TimeUnit.SECONDS), true, "Not enough message was broadcasted by the server");
		assertEquals(receivedEvent.get(), 2, "The client should have received two events");
		webSocket.close();
	}

	public class WebSocketListenerImpl implements WebSocketTextListener {

		@Override
		public void onOpen(WebSocket websocket) {}

		@Override
		public void onClose(WebSocket websocket) {}

		@Override
		public void onError(Throwable t) {}

		@Override
		public void onMessage(String message) {}

		@Override
		public void onFragment(String fragment, boolean last) {}

	}

}
