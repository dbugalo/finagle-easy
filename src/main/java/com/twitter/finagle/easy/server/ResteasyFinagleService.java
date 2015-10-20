package com.twitter.finagle.easy.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Map.Entry;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.core.Dispatcher;

import com.google.common.base.Preconditions;
import com.twitter.finagle.Service;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.util.Future;
import com.twitter.util.Promise;

/**
 * Implements the Finagle {@link com.twitter.finagle.Service} interface by
 * wrapping inbound requests and passing them into a RestEASY
 * {@link org.jboss.resteasy.core.Dispatcher}.
 * 
 * @author ed.peters
 * @author denis.rangel
 */
public class ResteasyFinagleService extends Service<Request, Response> {

	private static final Log LOG = LogFactory.getLog(ResteasyFinagleService.class);

	private final Dispatcher dispatcher;
	private final Executor executor;

	public ResteasyFinagleService(Dispatcher dispatcher, Executor executor) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
		this.executor = Preconditions.checkNotNull(executor, "executor");
	}

	/**
	 * Schedules a request for completion
	 * 
	 * @param request
	 *            an inbound Netty request
	 * @return a {@link Promise} that will return the result of completing the
	 *         request
	 */
	public Future<Response> apply(Request request) {
		Preconditions.checkNotNull(request, "request");
		LOG.info(String.format("inbound request %s %s", request.getMethod().getName(), request.getUri()));
		Promise<Response> promise = new Promise<Response>();
		this.executor.execute(new ResponseWorker(request, promise));

		return promise;
	}

	/**
	 * {@link Runnable} implementation that converts a Netty request to
	 * Resteasy, then uses the Resteasy Dispatcher to satisfy the call.
	 */
	protected class ResponseWorker implements Runnable {

		private final Request nettyRequest;
		private final Promise<Response> promise;

		public ResponseWorker(Request nettyRequest, Promise<Response> promise) {
			this.nettyRequest = nettyRequest;
			this.promise = promise;
		}

		@Override
		public void run() {
			final HttpVersion version = nettyRequest.getProtocolVersion();
			Response nettyResponse = null;
			try {
				nettyResponse = computeResponse(version);
			} catch (final Exception e) {
				LOG.info("unhandled error creating HTTP response", e);
				nettyResponse = new Response() {
					public HttpResponse httpResponse() {
						return new UnhandledErrorResponse(version, e);
					}
				};
			}
			LOG.info(String.format("outbound response %s", nettyResponse.getStatus()));
			this.promise.setValue(nettyResponse);
		}

		protected Response computeResponse(final HttpVersion version) {
			LOG.debug(String.format("incoming %s", nettyRequest.getUri()));
			for (Entry<String, String> entry : nettyRequest.headers().entries()) {
				LOG.debug(String.format("%s: %s", entry.getKey(), entry.getValue()));
			}
			LOG.debug(String.format("body: %s", nettyRequest.getContent().toString(UTF_8)));
			
			final InboundServiceRequest jaxrsRequest = new InboundServiceRequest(nettyRequest);
			final OutboundServiceResponse jaxrsResponse = new OutboundServiceResponse(version);

			final Response res = new Response() {
				public HttpResponse httpResponse() {
					return jaxrsResponse.getNettyResponse();
				}
			};

			dispatcher.invoke(jaxrsRequest, jaxrsResponse);

			return res;
		}

	}

}
