package com.twitter.finagle.easy.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;
import java.io.OutputStream;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * Used when we're hosting a Resteasy-annotated service implementation. Converts
 * Resteasy's {@link org.jboss.resteasy.spi.HttpResponse} to a Netty
 * {@link org.jboss.netty.handler.codec.http.HttpResponse}.
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class OutboundServiceResponse implements org.jboss.resteasy.spi.HttpResponse {

	// NOTE: a cleaner way to do this might be to subclass Netty's
	// DefaultHttpResponse, but then we couldn't implement the
	// JBoss interface because of all the method name clashes
	// TODO do we want cookie support? does netty even allow it?
	// TODO no clue how to get #isCommitted() from netty
	// TODO how would we support chunking?

	private final HttpResponse nettyResponse;
	private final MultivaluedMap<String, Object> headerWrapper;

	public OutboundServiceResponse(HttpVersion version) {
		this.nettyResponse = new DefaultHttpResponse(version, OK);
		this.nettyResponse.setChunked(false);
		this.nettyResponse.setContent(ChannelBuffers.dynamicBuffer());
		this.headerWrapper = new NettyHeaderWrapper(this.nettyResponse);
	}

	public HttpResponse getNettyResponse() {
		return this.nettyResponse;
	}

	@Override
	public int getStatus() {
		return this.nettyResponse.getStatus().getCode();
	}

	@Override
	public void sendError(int status) throws IOException {
		setStatus(status);
	}

	@Override
	public void sendError(int status, String message) throws IOException {
		this.nettyResponse.setStatus(new HttpResponseStatus(status, message));
	}

	@Override
	public void setStatus(int status) {
		this.nettyResponse.setStatus(HttpResponseStatus.valueOf(status));
	}

	/**
	 * @throws UnsupportedOperationException
	 *             (cookies aren't supported)
	 */
	@Override
	public void addNewCookie(NewCookie cookie) {
		throw new UnsupportedOperationException("addNewCookie");
	}

	@Override
	public MultivaluedMap<String, Object> getOutputHeaders() {
		return this.headerWrapper;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new ChannelBufferOutputStream(this.nettyResponse.getContent());
	}

	@Override
	public void setOutputStream(OutputStream os) {
		this.nettyResponse.setContent((ChannelBuffer) os);
	}
	
	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
		this.nettyResponse.headers().clear();
	}
}
