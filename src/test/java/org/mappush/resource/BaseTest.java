package org.mappush.resource;

import java.io.IOException;
import java.net.ServerSocket;

import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class BaseTest {

	protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

	protected AsyncHttpClient client;
	protected Nettosphere server;

	private String host;
	private int port;

	private int findFreePort() throws IOException {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	protected String getHttpUrl(String path) {
		return String.format("http://%s:%d/%s", host, port, path);
	}

	protected String getWsUrl(String path) {
		return String.format("ws://%s:%d/%s", host, port, path);
	}

	protected WebSocket getWebSocket() throws Exception {
		return getWebSocket(client.prepareGet(getWsUrl("")));
	}

	protected WebSocket getWebSocket(BoundRequestBuilder request) throws Exception {
		logger.info("Creating new WebSocket connection");
		WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().build();
		WebSocket webSocket = request.execute(handler).get();
		return webSocket;
	}

	@BeforeClass
	public void setUp() throws IOException {
		// Setup AsyncHttpClient
		AsyncHttpClientConfig ahcConfig = new AsyncHttpClientConfig.Builder().build();
		client = new AsyncHttpClient(ahcConfig);
		// Setup Nettosphere
		host = "127.0.0.1";
		port = findFreePort();
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
