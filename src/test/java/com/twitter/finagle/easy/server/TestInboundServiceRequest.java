package com.twitter.finagle.easy.server;

import static com.twitter.finagle.easy.AssertionHelpers.assertMultivaluedMapEquals;
import static com.twitter.finagle.easy.AssertionHelpers.assertUriInfoEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.twitter.finagle.httpx.Method;
import com.twitter.finagle.httpx.Request;

/**
 * Tests for the mapping of Netty requests to Resteasy.
 *
 * @author ed.peters
 */
public class TestInboundServiceRequest {

	public static final byte[] ENCODED_PARAMS = "k1=v1&k1=v2&k2=%3F".getBytes();

	@Test
	public void testRequestAttributes() throws Exception {
		Request nettyRequest = Request.apply(Method.apply("GET"), "/foo");
		HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
		assertNull("unexpected attribute", resteasyRequest.getAttribute("k"));
		resteasyRequest.setAttribute("k", this);
		assertSame("wrong attribute", this, resteasyRequest.getAttribute("k"));
		resteasyRequest.removeAttribute("k");
		assertNull("unexpected attribute", resteasyRequest.getAttribute("k"));
	}

	@Test
	public void testRequestHeaders() throws Exception {
		Request nettyRequest = Request.apply(Method.apply("GET"), "/foo");
		nettyRequest.headers().set("single", "a");
		nettyRequest.headers().set("multi", Arrays.asList("a", "b", "c"));
		HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
		assertMultivaluedMapEquals(resteasyRequest.getHttpHeaders().getRequestHeaders(),
				ImmutableMap.<String, Object> of("single", "a", "multi", Arrays.asList("a", "b", "c")));
	}

	@Test
	public void testRequestHeadersWithMultipleAccepts() throws Exception {
		Request nettyRequest = Request.apply(Method.apply("GET"), "/foo");
		nettyRequest.headers().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML + "," + MediaType.APPLICATION_JSON);
		HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
		List<MediaType> mediaTypes = resteasyRequest.getHttpHeaders().getAcceptableMediaTypes();
		assertEquals(2, mediaTypes.size());
		assertEquals(MediaType.APPLICATION_XML_TYPE, mediaTypes.get(0));
		assertEquals(MediaType.APPLICATION_JSON_TYPE, mediaTypes.get(1));
	}

	@Test
	public void testFormParameters() throws Exception {
		Request nettyRequest = Request.apply(Method.apply("POST"), "/foo");
		nettyRequest.setContent(ChannelBuffers.wrappedBuffer(ENCODED_PARAMS));
		HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
		assertMultivaluedMapEquals(resteasyRequest.getFormParameters(),
				ImmutableMap.<String, Object> of("k1", Arrays.asList("v1", "v2"), "k2", "%3F"));
		assertMultivaluedMapEquals(resteasyRequest.getDecodedFormParameters(),
				ImmutableMap.<String, Object> of("k1", Arrays.asList("v1", "v2"), "k2", "?"));
	}

	@Test
	public void testGetUriInfo() throws Exception {
		Request nettyRequest = Request.apply(Method.apply("GET"), "/foo?k=v&k=%3F");
		HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
		assertUriInfoEquals(resteasyRequest.getUri(), new URI("http://localhost:80/foo?k=v&k=%3F"), "/foo",
				ImmutableMap.<String, Object> of("k", Arrays.asList("v", "?")), new String[] { "foo" });
	}
}
