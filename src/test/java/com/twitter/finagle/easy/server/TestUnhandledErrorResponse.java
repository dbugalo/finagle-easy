package com.twitter.finagle.easy.server;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Test;

/**
 * Tests for our error response wrapper
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class TestUnhandledErrorResponse {

	@Test
	public void testSuccess() {
		String uuid = UUID.randomUUID().toString();
		Exception error = new Exception(uuid);
		HttpResponse response = new UnhandledErrorResponse(HTTP_1_1, error);
		assertEquals(500, response.getStatus().getCode());
		assertEquals(error.toString(), response.getStatus().getReasonPhrase());
	}

}
