package com.twitter.finagle.easy.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.twitter.common.quantity.Time.SECONDS;

import java.net.InetSocketAddress;
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
import com.twitter.common.zookeeper.ServerSet;
import com.twitter.common.zookeeper.ServerSetImpl;
import com.twitter.common.zookeeper.ZooKeeperClient;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientConfig.Yes;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Http;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.finagle.zookeeper.ZookeeperServerSetCluster;

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
	 * Default limit for the number of host connections
	 * 
	 * @see {@link ClientBuilder#hostConnectionLimit(int)}
	 */
	public static final int DEFAULT_HOST_CONNECTIONS = 1;

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

	private ClientExecutor executor;

	private Integer hostConnectionLimit;

	protected ClientBuilder() {
		this.hostConnectionLimit = DEFAULT_HOST_CONNECTIONS;
	}

	public ClientBuilder witHostConnectionLimit(Integer limit) {
		this.hostConnectionLimit = limit;
		return this;
	}

	/**
	 * Same as <code>withHttpClient(host, 80)</code>
	 */
	public ClientBuilder withHttpClient(String host) {
		return withHttpClient(host, 80);
	}

	/**
	 * Configures an HTTP client builder with the default host connection limit.
	 * 
	 * @param host
	 *            the remote host to connect to (e.g. "foo.bar.com")
	 * @param port
	 *            the remote port to connect to
	 * @return this (for chaining)
	 */
	public ClientBuilder withHttpClient(String host, int port) {
		checkNotNull(host, "host");
		checkArgument(port > 0, "invalid port " + port);
		LOG.info(String.format("new HTTP client for %s:%s", host, port));

		com.twitter.finagle.builder.ClientBuilder<Request, Response, Yes, Yes, Yes> builder = com.twitter.finagle.builder.ClientBuilder
				.get().codec(Http.get()).hostConnectionLimit(this.hostConnectionLimit)
				.hosts(new InetSocketAddress(host, port));
		return withClientBuilder(builder);
	}

	/**
	 * Same as <code>withZookeeperClient(zkHost, 2181, zkLocator)</code>
	 */
	public ClientBuilder withZookeeperClient(String zkHost, String zkLocator) {
		return withZookeeperClient(zkHost, 2181, zkLocator);
	}

	/**
	 * Configures a Zookeeper client builder with the default host connection
	 * limit and timeout.
	 * 
	 * @param zkHost
	 *            the hostname of a Zookeeper server to connect to
	 * @param zkPort
	 *            the port for the Zookeeper host
	 * @param zkLocator
	 *            the name-service path of the service to connect to
	 * @return this (for chaining)
	 */
	public ClientBuilder withZookeeperClient(String zkHost, Integer zkPort, String zkLocator) {
		checkNotNull(zkHost, "zkHost");
		checkNotNull(zkLocator, "zkLocator");
		
		LOG.info(String.format("new Zookeeper client for %s:%d %s", zkHost, zkPort, zkLocator));
		
		InetSocketAddress addr = new InetSocketAddress(zkHost, zkPort);
		ServerSet serverSet = new ServerSetImpl(new ZooKeeperClient(DEFAULT_ZK_TIMEOUT, addr), zkLocator);
		com.twitter.finagle.builder.ClientBuilder<Request, Response, Yes, Yes, Yes> builder = com.twitter.finagle.builder.ClientBuilder
				.get().codec(Http.get()).hostConnectionLimit(this.hostConnectionLimit)
				.cluster(new ZookeeperServerSetCluster(serverSet));
		return withClientBuilder(builder);
	}

	/**
	 * @param clientBuilder
	 *            an arbitrary {@link ClientBuilder} to use
	 * @return this (for chaining)
	 */
	public ClientBuilder withClientBuilder(
			com.twitter.finagle.builder.ClientBuilder<Request, Response, Yes, Yes, Yes> clientBuilder) {
		this.clientBuilder = clientBuilder;
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
		checkNotNull(this.clientBuilder, "clientBuilder");
		if (this.providerFactory == null) {
			this.providerFactory = ServiceUtils.getDefaultProviderFactory();
		}
		LOG.info(String.format("creating proxy with interface %s", serviceInterface.getName()));
		Service<Request, Response> service = com.twitter.finagle.builder.ClientBuilder.safeBuild(this.clientBuilder);
		this.executor = new FinagleBasedClientExecutor(this.providerFactory, service);

		return ProxyFactory.create(serviceInterface, DEFAULT_ENDPOINT_URI, executor, this.providerFactory);
	}

	public static ClientBuilder get() {
		return new ClientBuilder();
	}

}
