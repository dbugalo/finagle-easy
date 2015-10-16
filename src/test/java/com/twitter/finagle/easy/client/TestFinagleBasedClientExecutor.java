package com.twitter.finagle.easy.client;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response.Status;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;

import com.twitter.finagle.Service;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.util.Future;

/**
 * Tests the adapter between Finagle and Resteasy (this exercises the basic
 * control paths; edge cases of request/response mappings are covered in other
 * tests)
 *
 * @author ed.peters
 */
public class TestFinagleBasedClientExecutor {

	@Test
	@SuppressWarnings({ "resource", "deprecation", "rawtypes" })
	public void testHappyPath() throws Exception {

		ClientRequest resteasyRequest = new ClientRequest("/foo/bar");
		resteasyRequest.setHttpMethod("GET");
		resteasyRequest.accept(APPLICATION_JSON_TYPE);

		final Response nettyResponse = new Response() {
			@Override
			public HttpResponse httpResponse() {
				return new DefaultHttpResponse(HTTP_1_1, CONFLICT);
			}
		};

		Service<Request, Response> service = new Service<Request, Response>() {
			@Override
			public Future<Response> apply(Request request) {
				return Future.value(nettyResponse);
			}
		};

		ClientExecutor executor = new FinagleBasedClientExecutor(ServiceUtils.getDefaultProviderFactory(),
				service);

		ClientResponse resteasyResponse = executor.execute(resteasyRequest);
		assertNotNull("null response", resteasyRequest);
		
		assertEquals(Status.CONFLICT, resteasyResponse.getResponseStatus());
	}

}
