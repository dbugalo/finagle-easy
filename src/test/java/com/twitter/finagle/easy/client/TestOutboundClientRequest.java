package com.twitter.finagle.easy.client;

import static com.twitter.finagle.easy.AssertionHelpers.assertContentEquals;
import static com.twitter.finagle.easy.AssertionHelpers.assertHeadersEqual;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.jboss.resteasy.util.HttpHeaderNames.ACCEPT;
import static org.jboss.resteasy.util.HttpHeaderNames.CONTENT_LENGTH;
import static org.jboss.resteasy.util.HttpHeaderNames.CONTENT_TYPE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Tests Resteasy-to-Netty translation
 *
 * @author ed.peters
 */
public class TestOutboundClientRequest {

    public static final String CUSTOM_HEADER = "X-CustomHeader";

    @Test
    public void testSimpleGet() throws Exception {

        ClientRequest resteasyRequest = new ClientRequest("/foo/bar");
        resteasyRequest.setHttpMethod("GET");
        resteasyRequest.accept(APPLICATION_JSON_TYPE);

        HttpRequest nettyRequest = new OutboundClientRequest(resteasyRequest);
        assertRequestBasics(nettyRequest, HttpMethod.GET, "/foo/bar");
        assertHeadersEqual(nettyRequest, ImmutableMap.<String, Object>of(
                ACCEPT, APPLICATION_JSON,
                CONTENT_LENGTH, "0"
        ));
        assertContentEquals(nettyRequest.getContent(), null);
    }

    @Test
    public void testGetWithQueryParameters() throws Exception {

        UriBuilder builder = new ResteasyUriBuilder()
                .path("/foo/bar")
                .queryParam("k1", "v1")
                .queryParam("k2", "v2a", "v2b")
                .queryParam("k3", "?");

        ClientRequest resteasyRequest = new ClientRequest(builder,
                ClientRequest.getDefaultExecutor());
        resteasyRequest.setHttpMethod("GET");
        resteasyRequest.accept(APPLICATION_JSON_TYPE);

        HttpRequest nettyRequest = new OutboundClientRequest(resteasyRequest);
        assertRequestBasics(nettyRequest,
                HttpMethod.GET,
                "/foo/bar?k1=v1&k2=v2a&k2=v2b&k3=?");
        assertHeadersEqual(nettyRequest, ImmutableMap.<String, Object>of(
                ACCEPT, APPLICATION_JSON,
                CONTENT_LENGTH, "0"
        ));
        assertContentEquals(nettyRequest.getContent(), null);
    }

    @Test
    public void testGetWithCustomHeaders() throws Exception {

        ClientRequest resteasyRequest = new ClientRequest("/foo/bar");
        resteasyRequest.setHttpMethod("GET");
        resteasyRequest.accept(APPLICATION_JSON_TYPE);
        resteasyRequest.header("single", "a");
        resteasyRequest.header("multi", "a");
        resteasyRequest.header("multi", "b");

        HttpRequest nettyRequest = new OutboundClientRequest(resteasyRequest);
        assertRequestBasics(nettyRequest, HttpMethod.GET, "/foo/bar");
        assertHeadersEqual(nettyRequest, ImmutableMap.<String,Object>of(
                ACCEPT, APPLICATION_JSON,
                "single", "a",
                "multi", Arrays.asList("a", "b"),
                CONTENT_LENGTH, "0"
        ));
        assertContentEquals(nettyRequest.getContent(), null);
    }

    @Test
    public void testSimplePost() throws Exception {

        byte [] expectedContent = UUID.randomUUID().toString().getBytes();

        ClientRequest resteasyRequest = new ClientRequest("/foo/bar");
        resteasyRequest.setHttpMethod("POST");
        resteasyRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        resteasyRequest.body(MediaType.APPLICATION_JSON_TYPE, expectedContent);

        HttpRequest nettyRequest = new OutboundClientRequest(resteasyRequest);
        assertRequestBasics(nettyRequest, HttpMethod.POST, "/foo/bar");
        assertHeadersEqual(nettyRequest, ImmutableMap.<String,Object>of(
                ACCEPT, APPLICATION_JSON,
                CONTENT_TYPE, APPLICATION_JSON,
                CONTENT_LENGTH, Integer.toString(expectedContent.length)
        ));
        assertContentEquals(nettyRequest.getContent(), expectedContent);
    }

    @Test
    public void testPostWithCustomProvider() throws Exception {
    	CustomWriter writer = new CustomWriter();
        ResteasyProviderFactory factory = new ResteasyProviderFactory();
        factory.registerProvider(CustomWriter.class);

        String suppliedContent = UUID.randomUUID().toString();
        byte [] expectedContent = CustomWriter.instance.toString().getBytes();

        ClientRequest resteasyRequest = new ClientRequest(
                new ResteasyUriBuilder().path("/foo/bar"),
                ClientRequest.getDefaultExecutor(),
                factory);
        resteasyRequest.setHttpMethod("POST");
        resteasyRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        resteasyRequest.body(MediaType.APPLICATION_JSON_TYPE, suppliedContent);

        HttpRequest nettyRequest = new OutboundClientRequest(resteasyRequest);
        assertRequestBasics(nettyRequest, HttpMethod.POST, "/foo/bar");
        assertHeadersEqual(nettyRequest, ImmutableMap.<String, Object>of(
                ACCEPT, APPLICATION_JSON,
                CONTENT_TYPE, APPLICATION_JSON,
                CUSTOM_HEADER, suppliedContent,
                CONTENT_LENGTH, expectedContent.length
        ));
        
        assertContentEquals(nettyRequest.getContent(), expectedContent);
    }

    protected void assertRequestBasics(HttpRequest nettyRequest,
                                       HttpMethod expectedMethod,
                                       String expectedPath) {
        assertEquals(expectedMethod, nettyRequest.getMethod());
        assertEquals(HttpVersion.HTTP_1_1, nettyRequest.getProtocolVersion());
        assertEquals(expectedPath, nettyRequest.getUri());
    }

    /**
     * Custom writer that modifies body content and message header
     */
    public static class CustomWriter implements MessageBodyWriter<Object> {
    	
    	private static CustomWriter instance;
    	
    	public CustomWriter() {
    		CustomWriter.instance = this;
		}
    	
        @Override
        public long getSize(
                Object object,
                Class<?> type,
                Type genericType,
                Annotation [] annotations,
                MediaType mediaType) {
            return -1L;
        }

        @Override
        public boolean isWriteable(
                Class<?> type,
                Type genericType,
                Annotation [] annotations,
                MediaType mediaType) {
            return true;
        }

        @Override
        public void writeTo(
                Object object,
                Class<?> type,
                Type genericType,
                Annotation [] annotations,
                MediaType mediaType,
                MultivaluedMap<String,Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            String data = object.toString();
            httpHeaders.putSingle(CUSTOM_HEADER, data);
            entityStream.write(this.toString().getBytes());
        }

    }

}
