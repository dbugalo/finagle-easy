package com.twitter.finagle.easy.server;

import static com.twitter.finagle.easy.AssertionHelpers.assertContentEquals;
import static com.twitter.finagle.easy.AssertionHelpers.assertHeaderEquals;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.junit.Test;

import com.twitter.finagle.Service;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Method;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.util.Future;

/**
 * Tests the paths through the finagle service implementation (this exercises
 * basic control paths; edge cases of request/response mappings are covered in
 * other tests)
 *
 * @author ed.peters
 */
public class TestResteasyFinagleService {

	// everyone gives their classes the same name, so we are just going
	// to tough it out with fully-qualified names, to make things nice
	// and explicit
	protected Request nettyRequest;
	protected Response nettyResponse;

	protected org.jboss.resteasy.spi.HttpRequest resteasyRequest;
	protected org.jboss.resteasy.spi.HttpResponse resteasyResponse;

	@Test
	public void testHappyPath() throws Exception {
		final byte[] expectedContent = UUID.randomUUID().toString().getBytes();
		this.nettyRequest = Request.apply(Method.apply("GET"), "/foo");

		this.nettyRequest.headers().set("single", "a");
		invoke(new Runnable() {
			@Override
			public void run() {
				resteasyResponse.setStatus(200);
				resteasyResponse.getOutputHeaders().putSingle("single", "b");
				try {
					resteasyResponse.getOutputStream().write(expectedContent);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		assertNotNull("no input resteasy message", this.resteasyRequest);
		assertNotNull("no output resteasy message", this.resteasyResponse);
		assertNotNull("no output netty message", this.nettyResponse);
		assertEquals("wrong method", "GET", resteasyRequest.getHttpMethod());
		assertEquals("wrong path", "/foo", resteasyRequest.getUri().getPath());
		assertHeaderEquals("wrong header", "a", resteasyRequest.getHttpHeaders().getRequestHeader("single"));
		assertEquals("wrong code", 200, nettyResponse.getStatus().getCode());
		assertHeaderEquals("wrong header", "b", nettyResponse.headers().getAll("single"));
		assertContentEquals(nettyResponse.getContent(), expectedContent);
	}

	@Test
	public void testRequestReadingError() throws Exception {
		final RuntimeException error = new RuntimeException("expected error");
		this.nettyRequest = newRequest(new DefaultHttpRequest(HTTP_1_1, GET, "/foo") {
			@Override
			public ChannelBuffer getContent() {
				throw error;
			}
		});
		invoke(new Runnable() {
			@Override
			public void run() {
				fail("we should never get here");
			}
		});
		assertNull("unexpected resteasy input", this.resteasyRequest);
		assertNull("unexpected resteasy output", this.resteasyResponse);
		assertNotNull("no output netty message", this.nettyResponse);
		assertEquals("wrong code", 500, nettyResponse.getStatus().getCode());
		assertEquals("wrong message", error.toString(), nettyResponse.getStatus().getReasonPhrase());
	}

	@Test
	public void testUnhandledDispatchError() throws Exception {
		final RuntimeException error = new RuntimeException("expected error");
		this.nettyRequest = Request.apply(Method.apply("GET"), "/foo");
		this.nettyRequest.headers().set("single", "a");
		invoke(new Runnable() {
			@Override
			public void run() {
				throw error;
			}
		});
		assertNotNull("no input resteasy message", this.resteasyRequest);
		assertNotNull("no output resteasy message", this.resteasyResponse);
		assertNotNull("no output netty message", this.nettyResponse);
		assertEquals("wrong method", "GET", resteasyRequest.getHttpMethod());
		assertEquals("wrong path", "/foo", resteasyRequest.getUri().getPath());
		assertHeaderEquals("wrong header", "a", resteasyRequest.getHttpHeaders().getRequestHeader("single"));
		assertEquals("wrong code", 500, nettyResponse.getStatus().getCode());
		assertEquals("wrong message", error.toString(), nettyResponse.getStatus().getReasonPhrase());
	}

	/*
	 * Sets up the service plumbing and invokes the supplied runner in the
	 * middle of the processing chain. We're ignoring generic types because it
	 * would make the resulting code insanely hard to read.
	 */
	@SuppressWarnings("deprecation")
	protected void invoke(final Runnable runner) {
		assertNotNull("no input netty message", this.nettyRequest);
		Service<Request, Response> service = new ResteasyFinagleService(new MockDispatcher(runner), Executors.newSingleThreadExecutor());
		Future<Response> future = service.apply(this.nettyRequest);
		this.nettyResponse = future.get();
	}

	/**
	 * Specialized dispatcher that captures its input and output, and
	 * short-circuits all processing in favor of invoking a simple Runnable.
	 */
	protected class MockDispatcher extends SynchronousDispatcher {

		private final Runnable runner;

		protected MockDispatcher(Runnable runner) {
			super(ServiceUtils.getDefaultProviderFactory());
			this.runner = runner;
		}

		@Override
		public void invoke(HttpRequest request, HttpResponse response) {
			resteasyRequest = request;
			resteasyResponse = response;
			this.runner.run();
		}

	}

	private Request newRequest(final DefaultHttpRequest httpRequest) {
		final Request req = new Request() {

			@Override
			public org.jboss.netty.handler.codec.http.HttpRequest httpRequest() {
				return httpRequest;
			}

			@Override
			public InetSocketAddress remoteSocketAddress() {
				// TODO Auto-generated method stub
				return null;
			}
		};

		return req;
	}
}
