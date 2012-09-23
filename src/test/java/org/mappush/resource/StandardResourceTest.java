package org.mappush.resource;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import com.ning.http.client.Response;

public class StandardResourceTest extends BaseTest {

	@Test(enabled = true)
	public void postEvent() throws Exception {
		// setup
		final String event = "{\"lat\":1.234,\"lng\":1.234}";
		// test
		Response response = client.preparePost(getHttpUrl("event"))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setBody(event)
				.execute().get();
		// assert
		assertEquals(response.getStatusCode(), 200);
	}

	@Test(enabled = true)
	public void postEvent_withInvalidData() throws Exception {
		// setup
		final String event = "{\"latitude\":0.0,\"longitude\":0.0}";
		// test
		Response response = client.preparePost(getHttpUrl("event"))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.setBody(event)
				.execute().get();
		// assert
		assertEquals(response.getStatusCode(), 400);
	}

}
