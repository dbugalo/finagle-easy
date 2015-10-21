package com.twitter.finagle.easy.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.twitter.common.quantity.Time.SECONDS;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.google.common.base.Throwables;
import com.twitter.common.quantity.Amount;
import com.twitter.common.quantity.Time;
import com.twitter.finagle.Httpx;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientConfig.Yes;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;

/**
 * Fluent-style builder for clients that wrap a Finagle client with an annotated
 * service interface via Resteasy.
 *
 * @author denis.rangel
 * @author ed.peters
 * @author jeff
 */
public class ClientBuilder {

	/**
	 * Default timeout for Zookeeper connections
	 */
	public static final Amount<Integer, Time> DEFAULT_ZK_TIMEOUT = Amount.of(1, SECONDS);

	private static final Log LOG = LogFactory.getLog(ClientBuilder.class);

	/*
	 * Resteasy insists on having a real endpoint URL. Since we're handling the
	 * invoke through Finagle, this doesn't really matter, so we'll use a dummy
	 * endpoint to keep it happy.
	 */
	private static final URI DEFAULT_ENDPOINT_URI;

	static {
		try {
			DEFAULT_ENDPOINT_URI = new URI("http://localhost/");
		} catch (URISyntaxException e) {
			throw Throwables.propagate(e);
		}
	}

	private ResteasyProviderFactory providerFactory;

	private com.twitter.finagle.builder.ClientBuilder<Request, Response, Yes, Yes, Yes> clientBuilder;

	private Service<Request, Response> service;
	
	private ClientExecutor executor;

	protected ClientBuilder() {
		this.providerFactory = ServiceUtils.getDefaultProviderFactory();
	}

	public ClientBuilder withService(String uri) {
		this.service = Httpx.newService(uri);
		
		return this;
	}

	/**
	 * @param providerFactory
	 *            an arbitrary {@link ResteasyProviderFactory} to use
	 * @return this (for chaining)
	 */
	public ClientBuilder withProviderFactory(ResteasyProviderFactory providerFactory) {
		this.providerFactory = providerFactory;
		return this;
	}

	public void close() throws Exception {
		this.executor.close();
	}

	public <T> T build(Class<T> serviceInterface) {
		checkNotNull(this.service, "clientBuilder");

		LOG.info(String.format("creating proxy with interface %s", serviceInterface.getName()));
		
		this.executor = new FinagleBasedClientExecutor(this.providerFactory, service);
		return ProxyFactory.create(serviceInterface, DEFAULT_ENDPOINT_URI, executor, this.providerFactory);
	}

	public static ClientBuilder get() {
		return new ClientBuilder();
	}

}
