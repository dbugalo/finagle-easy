package com.twitter.finagle.easy;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMessage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Helpers for different types of assertions that are common across these tests
 *
 * @author ed.peters
 * @author denis.rangel
 */
public final class AssertionHelpers {

	private AssertionHelpers() {
	}

	/**
	 * Most of the time headers have a single value, but they can be
	 * multi-valued. This glosses over the difference.
	 */
	@SuppressWarnings("rawtypes")
	public static void assertHeaderEquals(String message, Object expected, List<?> actual) {
		List<?> expectedList = null;
		if (expected == null) {
			expectedList = null;
		} else if (expected instanceof List) {
			expectedList = (List) expected;
		} else {
			expectedList = Arrays.asList(expected.toString());
		}
		assertEquals(message, expectedList, actual);
	}

	public static void assertHeadersEqual(HttpMessage nettyMessage, Map<String, Object> expected) {
		assertEquals("wrong header names", expected.keySet(), nettyMessage.headers().names());
		for (String key : expected.keySet()) {
			assertHeaderEquals("wrong value for header " + key, expected.get(key), nettyMessage.headers().getAll(key));
		}
	}

	public static void assertUriInfoEquals(UriInfo info, URI absolutePath, String path,
			Map<String, Object> expectedParams, String[] expectedSegments) {

		assertEquals("wrong path", path, info.getPath());

		assertMultivaluedMapEquals(info.getQueryParameters(), expectedParams);

		List<PathSegment> actualSegments = info.getPathSegments();
		assertNotNull("path segments are null", actualSegments);
		assertEquals("wrong number of segments in " + actualSegments, expectedSegments.length, actualSegments.size());
		for (int i = 0; i < actualSegments.size(); i++) {
			assertEquals("path disagrees at segment " + i, expectedSegments[i], actualSegments.get(i).getPath());
		}

	}

	public static void assertMultivaluedMapEquals(MultivaluedMap<String, ?> actual, Map<String, Object> expected) {
		assertEquals("wrong keys", expected.keySet(), actual.keySet());
		for (String key : actual.keySet()) {
			Object expectedVal = expected.get(key);
			if (expectedVal instanceof String) {
				expectedVal = Arrays.asList(expectedVal.toString());
			}
			assertEquals("wrong value for " + key, expectedVal, actual.get(key));
		}
	}

	public static void assertContentEquals(ChannelBuffer buffer, byte[] expectedBytes) throws Exception {
		assertNotNull("buffer is null", buffer);
		if (expectedBytes == null || expectedBytes.length == 0) {
			assertEquals("unexpected content", 0, buffer.readableBytes());
		} else {
			assertEquals("wrong content length", expectedBytes.length, buffer.readableBytes());
			assertArrayEquals("wrong content", expectedBytes,
					IOUtils.toByteArray(new ChannelBufferInputStream(buffer)));
		}
	}
}
