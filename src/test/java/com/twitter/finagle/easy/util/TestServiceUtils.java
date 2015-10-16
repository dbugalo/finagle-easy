package com.twitter.finagle.easy.util;

import static com.twitter.finagle.easy.AssertionHelpers.assertHeaderEquals;
import static com.twitter.finagle.easy.AssertionHelpers.assertMultivaluedMapEquals;
import static com.twitter.finagle.easy.AssertionHelpers.assertUriInfoEquals;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_LANGUAGE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
//import com.twitter.finagle.httpx.Request.MockRequest;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Method;
import com.twitter.finagle.httpx.Request;

/**
 * Testing for header-related utilities.
 *
 * @author ed.peters
 */
public class TestServiceUtils {

    public static final String UTF8 = "UTF-8";

    public static final String JSON_UTF8 = APPLICATION_JSON + ";charset=UTF-8";

    @Test
    public void testNewMultiValuedMap() throws Exception {
        MultivaluedMap<String,String> actual = ServiceUtils.newMultiValuedMap(
                "single-value", "a",
                "multi-value", Arrays.asList("a", "b", "c")
        );
        assertMultivaluedMapEquals(actual, ImmutableMap.<String,Object>of(
            "single-value", "a",
            "multi-value", Arrays.asList("a", "b", "c")
        ));
    }

    @Test
    public void testToMultimap() {

        MultivaluedMap<String,String> expected = ServiceUtils.newMultiValuedMap(
                "single-value", "value",
                "multi-value", ImmutableList.of("a", "b", "c"),
                ACCEPT, APPLICATION_JSON,
                ACCEPT_LANGUAGE, "en");

        Request request = Request.apply(Method.apply("GET"), "/foo");
        request.setContentType(APPLICATION_JSON, UTF8);
        for (String key : expected.keySet()) {
            request.headers().add(key, expected.get(key));
        }

        MultivaluedMap<String,String> actual = ServiceUtils.toMultimap(request.getHttpMessage());
        assertNotNull("returned null headers", actual);
        for (String key : expected.keySet()) {
            Object expectedValue = expected.get(key);
            if (expectedValue instanceof String) {
                expectedValue = Arrays.asList(expectedValue);
            }
            assertEquals(key + " lookup failed",
                    expectedValue,
                    actual.get(key));
            assertEquals(key + " case-insensitive lookup failed",
                    expectedValue,
                    actual.get(key.toLowerCase()));
        }
        assertHeaderEquals("content type lookup failed",
                JSON_UTF8,
                actual.get(CONTENT_TYPE));
        assertIsMutable(CONTENT_TYPE, actual.get(CONTENT_TYPE));
        assertIsMutable(ACCEPT_LANGUAGE, actual.get(ACCEPT_LANGUAGE));
    }

    @Test
    public void testToHeaders() throws Exception {

        MultivaluedMap<String,String> expected = ServiceUtils.newMultiValuedMap(
                "single-value", "value",
                "multi-value", ImmutableList.of("a", "b", "c"),
                ACCEPT, APPLICATION_JSON,
                ACCEPT_LANGUAGE, "en",
                CONTENT_TYPE, APPLICATION_JSON);

        HttpHeaders actual = ServiceUtils.toHeaders(expected);
        assertNotNull("returned null headers", actual);
        for (String key : Arrays.asList("single-value", "multi-value")) {
            assertHeaderEquals(key + " lookup failed",
                    expected.get(key),
                    actual.getRequestHeader(key));
            assertHeaderEquals(key + " case-insensitive lookup failed",
                    expected.get(key),
                    actual.getRequestHeader(key.toLowerCase()));
        }
        assertEquals("content-type lookup failed",
                APPLICATION_JSON_TYPE,
                actual.getMediaType());
        assertEquals("accept-language lookup failed",
                Arrays.asList(Locale.ENGLISH),
                actual.getAcceptableLanguages());
        assertEquals("accept lookup failed",
                Arrays.asList(APPLICATION_JSON_TYPE),
                actual.getAcceptableMediaTypes());
    }

    @Test
    public void testExtractUriInfoWithBasePath() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/", "foo", 100, false);
        assertUriInfoEquals(info,
                new URI("http://foo:100/"),
                "/",
                Collections.<String,Object>emptyMap(),
                new String[]{""});
    }

    @Test
    public void testExtractUriInfoWithSimplePath() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/a", "foo", 101, true);
        assertUriInfoEquals(info,
                new URI("https://foo:101/a"),
                "/a",
                Collections.<String,Object>emptyMap(),
                new String[]{"a"});
    }

    @Test
    public void testExtractUriInfoWithDeepPath() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/a/b/c", null, 80, false);
        assertUriInfoEquals(info,
                new URI("http://localhost:80/a/b/c"),
                "/a/b/c",
                Collections.<String,Object>emptyMap(),
                new String[]{"a", "b", "c"});
    }

    @Test
    public void testExtractUriInfoWithSimpleParams() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/a?k=v", "foo", 80, true);
        assertUriInfoEquals(info,
                new URI("https://foo:80/a?k=v"),
                "/a",
                ImmutableMap.<String,Object>of("k", "v"),
                new String[]{"a"});
    }

    @Test
    public void testExtractUriInfoWithMultiValueParams() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/a?k=v1&k=v2", null, 80, true);
        assertUriInfoEquals(info,
                new URI("https://localhost:80/a?k=v1&k=v2"),
                "/a",
                ImmutableMap.<String,Object>of("k", Arrays.asList("v1", "v2")),
                new String[]{ "a" });
    }

    @Test
    public void testExtractUriInfoWithEncodedParams() throws Exception {
        UriInfo info = ServiceUtils.toUriInfo("/a?k=%3F", null, 80, false);
        assertUriInfoEquals(info,
                new URI("http://localhost:80/a?k=%3F"),
                "/a",
                ImmutableMap.<String,Object>of("k", "?"),
                new String[]{"a"});
    }

    protected void assertIsMutable(String key, List<String> list) {
        assertNotNull(key + " is null", list);
        try {
            list.add("foo");
        }
        catch (Exception e) {
            fail(key + " is not mutable: " + e.toString());
        }
    }
}
