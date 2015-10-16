package com.twitter.finagle.easy.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.Encode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Request;

/**
 * Used when we're hosting a Resteasy-annotated service implementation.
 * Implements Resteasy's {@link org.jboss.resteasy.spi.HttpRequest} interface on
 * top of a Netty {@link org.jboss.netty.handler.codec.http.HttpRequest}
 *
 * Currently doesn't support asynchronous processing.
 *
 * @author ed.peters
 * @author denis.rangel
 *
 * @see "http://bill.burkecentral.com/2008/10/09/jax-rs-asynch-http/"
 */
public class InboundServiceRequest implements org.jboss.resteasy.spi.HttpRequest {

	private final Request nettyRequest;
	private final Map<String, Object> attributeMap;
	private final HttpHeaders jaxrsHeaders;
	private final UriInfo jaxrsUriInfo;
	private InputStream overrideStream;
	private InputStream underlyingStream;
	private MultivaluedMap<String, String> rawFormParams;
	private MultivaluedMap<String, String> decodedFormParams;

	public InboundServiceRequest(Request nettyRequest) {
		this.nettyRequest = nettyRequest;
		this.jaxrsHeaders = ServiceUtils.toHeaders(ServiceUtils.toMultimap(nettyRequest.getHttpMessage()));
		this.jaxrsUriInfo = new ResteasyUriInfo(ResteasyUriBuilder.fromUri(nettyRequest.getUri()).build());
		this.attributeMap = Maps.newHashMap();
		this.overrideStream = null;
		this.underlyingStream = new ChannelBufferInputStream(nettyRequest.getContent());
	}

	@Override
	public Object getAttribute(String name) {
		Preconditions.checkNotNull(name, "name");
		return this.attributeMap.get(name);
	}

	@Override
	public void removeAttribute(String name) {
		Preconditions.checkNotNull(name, "name");
		this.attributeMap.remove(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		Preconditions.checkNotNull(name, "name");
		this.attributeMap.put(name, value);
	}

	@Override
	public String getHttpMethod() {
		return this.nettyRequest.getMethod().getName();
	}

	@Override
	public HttpHeaders getHttpHeaders() {
		return this.jaxrsHeaders;
	}

	@Override
	public InputStream getInputStream() {
		// this is the same way RestEASY implements this on top of
		// HttpServletRequests: as a temporary override of the underlying
		// input stream
		return this.overrideStream == null ? this.underlyingStream : this.overrideStream;
	}

	@Override
	public void setInputStream(InputStream stream) {
		this.overrideStream = stream;
	}

	@Override
	public MultivaluedMap<String, String> getDecodedFormParameters() {
		readFormParams();
		return this.decodedFormParams;
	}

	@Override
	public MultivaluedMap<String, String> getFormParameters() {
		readFormParams();
		return this.rawFormParams;
	}

	// since parsing form parameters requires reading the request body, we need
	// to
	// synchronize it so it happens only once
	protected synchronized void readFormParams() {
		if (this.rawFormParams == null) {
			try {
				this.rawFormParams = FormUrlEncodedProvider.parseForm(getInputStream());
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
			this.decodedFormParams = Encode.decode(this.rawFormParams);
		}
	}

	@Override
	public boolean isInitial() {
		// true indicates that this is a regular request being handled for
		// the first time (as opposed to an asynchronous request being re-sent
		// through the stack)
		return true;
	}

	@Override
	public MultivaluedMap<String, String> getMutableHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResteasyUriInfo getUri() {
		return (ResteasyUriInfo) this.jaxrsUriInfo;
	}

	@Override
	public void setHttpMethod(String method) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRequestUri(URI requestUri) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public ResteasyAsynchronousContext getAsyncContext() {
		// TODO Auto-generated method stub
		return new ResteasyAsynchronousContext() {

			@Override
			public boolean isSuspended() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public ResteasyAsynchronousResponse getAsyncResponse() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ResteasyAsynchronousResponse suspend() throws IllegalStateException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ResteasyAsynchronousResponse suspend(long millis) throws IllegalStateException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ResteasyAsynchronousResponse suspend(long time, TimeUnit unit) throws IllegalStateException {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	@Override
	public void forward(String path) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean wasForwarded() {
		// TODO Auto-generated method stub
		return false;
	}
}
