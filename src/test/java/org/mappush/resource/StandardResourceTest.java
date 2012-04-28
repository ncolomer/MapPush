package org.mappush.resource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class StandardResourceTest extends BaseTest {
	
	private static final Logger logger = LoggerFactory.getLogger(StandardResourceTest.class);

	@Test(enabled=true)
	public void testNettosphereJerseyConfig() throws Exception {
		logger.info("{}: running testNettosphereJerseyConfig", getClass().getSimpleName());
		Request req = client.prepareGet(getHttpUrl("initparams")).build();
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
		logger.info("{}: running testPostValidEvent", getClass().getSimpleName());
		final String sentEvent = "{\"lat\":0.0,\"lng\":0.0}";
		Response resp = client.preparePost(getHttpUrl("event"))
				.setBody(sentEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertNotNull(resp, "Response should not be null");
		assertEquals(resp.getStatusCode(), 200, "Response status code should be 200");
	}

	@Test(enabled=true)
	public void testPostNonValidEvent() throws Exception {
		logger.info("{}: running testPostNonValidEvent", getClass().getSimpleName());
		final String sentEvent = "{\"latitude\":0.0,\"longitude\":0.0}";
		Response resp = client.preparePost(getHttpUrl("event"))
				.setBody(sentEvent).setHeader("Content-Type", MediaType.APPLICATION_JSON)
				.execute().get();
		assertNotNull(resp, "Response should not be null");
		assertEquals(resp.getStatusCode(), 400, "Response status code should be 400");
	}

}
