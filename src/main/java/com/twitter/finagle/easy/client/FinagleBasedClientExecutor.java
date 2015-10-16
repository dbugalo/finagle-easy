package com.twitter.finagle.easy.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Map.Entry;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.twitter.finagle.Service;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;

/**
 * Implementation of Resteasy {@link org.jboss.resteasy.client.ClientExecutor}
 * interface on top of a Finagle {@link com.twitter.finagle.Service}.  This
 * allows us to make outbound calls using a Resteasy proxy.
 *
 * TODO is there any benefit to delaying the call to Future.get?
 * TODO can we be more efficent about the conversion of ChannelBuffers to bytes and back?
 *
 * @author ed.peters
 */
public class FinagleBasedClientExecutor implements ClientExecutor, Closeable {

    private static final Log LOG =
            LogFactory.getLog(FinagleBasedClientExecutor.class);

    private final ResteasyProviderFactory providerFactory;
    private final Service<Request, Response> finagleService;

    public FinagleBasedClientExecutor(
            ResteasyProviderFactory providerFactory,
            Service<Request, Response> finagleService) {
        this.providerFactory = providerFactory;
        this.finagleService = finagleService;
    }

    @Override
    public ClientRequest createRequest(UriBuilder uriBuilder) {
        return new ClientRequest(uriBuilder, this, this.providerFactory);
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return createRequest(new ResteasyUriBuilder().uriTemplate(uriTemplate));
    }

    @Override
    public ClientResponse execute(final ClientRequest resteasyRequest)
            throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("outbound "
                    + resteasyRequest.getHttpMethod() + " "
                    + resteasyRequest.getUri());
            for (String name : resteasyRequest.getHeaders().keySet()) {
                LOG.debug(name + ": " + resteasyRequest.getHeaders().get(name));
            }
        }

        Request nettyRequest = null;
        try {
            nettyRequest = new Request() {
            	private HttpRequest httpRequest = new OutboundClientRequest(resteasyRequest);
            	
				@Override
				public HttpRequest httpRequest() {
					try {
						return httpRequest;
					} catch (Exception e) {
					}
					
					return null;
				}

				@Override
				public InetSocketAddress remoteSocketAddress() {
					// TODO Auto-generated method stub
					return null;
				}
            	
            };
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "error converting outbound request (Resteasy --> Netty)", e);
        }

        Response nettyResponse = null;
        try {
            nettyResponse = this.finagleService.apply(nettyRequest).get();
        }
        catch (Exception e) {
            throw new RuntimeException("error invoking Finagle service", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("inbound " + nettyResponse.getStatus());
            for (Entry<String, String> entry : nettyResponse.headers()) {
                LOG.debug(entry.getKey() + ": " + entry.getValue());
            }
        }

        ClientResponse response = null;
        try {
            response = new InboundClientResponse(nettyResponse,
                    this.providerFactory);
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "error converting inbound response (Netty --> Resteasy)", e);
        }

        return response;
    }

    @Override
    public void close() {
    	this.finagleService.close();
    }
}
