package com.twitter.finagle.easy.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.twitter.common.quantity.Time.SECONDS;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.core.AcceptHeaderByFileSuffixFilter;
import org.jboss.resteasy.core.AsynchronousDispatcher;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.twitter.common.quantity.Amount;
import com.twitter.common.quantity.Time;
import com.twitter.finagle.Service;
import com.twitter.finagle.easy.util.ServiceUtils;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;

/**
 * Builder for a Finagle {@link com.twitter.finagle.Service} that knows how to
 * dispatch to a RestEASY {@link org.jboss.resteasy.core.Dispatcher}. Also knows
 * how to pull service beans from a Spring bean factory.
 *
 * TODO what other aspects of RestEASY do we want to be able to customize?
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class ServiceBuilder {

	/**
	 * Default timeout for Zookeeper connections
	 */
	public static final Amount<Integer, Time> DEFAULT_ZK_TIMEOUT = Amount.of(1, SECONDS);
	
	private static final Log LOG = LogFactory.getLog(ServiceBuilder.class);
	
	/**
	 * Default set of file-extension-to-MIME-type mappings
	 */
	public static final Map<String, MediaType> DEFAULT_MEDIA_TYPES = ImmutableMap.of("json",
			MediaType.APPLICATION_JSON_TYPE, "bin", MediaType.APPLICATION_OCTET_STREAM_TYPE, "html",
			MediaType.TEXT_HTML_TYPE, "xml", MediaType.TEXT_XML_TYPE);

	private ResteasyProviderFactory providerFactory;
	private Map<String, MediaType> mediaTypes;
	private Map<String, String> languages;
	private List<Object> beans;
	private Executor executor;

	protected ServiceBuilder() {
		this.mediaTypes = Maps.newHashMap(DEFAULT_MEDIA_TYPES);
		this.languages = Maps.newHashMap();
		this.beans = Lists.newArrayList();
		this.providerFactory = ServiceUtils.getDefaultProviderFactory();
		this.executor = Executors.newSingleThreadExecutor();
	}

	/**
	 * Sets the size of the background thread pool
	 * 
	 * @param size
	 *            fixed number of threads for request handling pool
	 * @return this (for chaining)
	 */
	public ServiceBuilder withThreadPoolSize(int size) {
		return withExecutor(Executors.newFixedThreadPool(size));
	}

	/**
	 * Sets a custom executor to be used for handling calls
	 * 
	 * @param executor
	 *            the executor to use
	 * @return this (for chaining)
	 */
	public ServiceBuilder withExecutor(Executor executor) {
		this.executor = executor;
		return this;
	}

	/**
	 * Adds a REST-annotated bean to the dispatcher for this service
	 * 
	 * @param endpoint
	 *            a service bean
	 * @return this (for chaining)
	 * @throws NullPointerException
	 *             if the supplied bean is null
	 * @throws IllegalArgumentException
	 *             if the supplied bean isn't a root resource, as reported by
	 *             {@link GetRestful}
	 */
	public ServiceBuilder withEndpoint(Object endpoint) {
		checkNotNull(endpoint, "endpoint is null");
		checkArgument(GetRestful.isRootResource(endpoint.getClass()), "endpoint is not a root resource");
		this.beans.add(endpoint);
		return this;
	}

	/**
	 * Same as calling {@link #withEndpoint(Object)} on each bean in the
	 * supplied list
	 */
	public ServiceBuilder withEndpoints(Object... beans) {
		for (Object bean : beans) {
			withEndpoint(bean);
		}
		return this;
	}

	/**
	 * Same as calling {@link #withEndpoint(Object)} on each bean in the
	 * supplied collection
	 */
	public ServiceBuilder withEndpoints(Collection<?> beans) {
		for (Object bean : beans) {
			withEndpoint(bean);
		}
		return this;
	}

	
	/**
	 * Adds a new file-extension-to-MIME-type mapping to the default set for
	 * this service
	 * 
	 * @param ext
	 *            a file extension without the dot (e.g. "xml")
	 * @param mediaType
	 *            the corresponding "Accept" header to infer for requests ending
	 *            with that file extension
	 */
	public ServiceBuilder withMediaTypeMapping(String ext, MediaType mediaType) {
		this.mediaTypes.put(ext, mediaType);
		return this;
	}

	/**
	 * Adds a new file-extension-to-language mapping to the default set for this
	 * service
	 * 
	 * @param ext
	 *            a file extension without the dot (e.g. "en")
	 * @param lang
	 *            the corresponding "Accept-Language" header to infer for
	 *            requests ending with that extension (e.g. "en")
	 */
	public ServiceBuilder withLanguageMapping(String ext, String lang) {
		this.languages.put(ext, lang);
		return this;
	}

	/**
	 * @return a new service
	 */
	public Service<Request, Response> build() {
		checkArgument(!this.beans.isEmpty(), "Beans");
		Dispatcher dispatcher = new AsynchronousDispatcher(this.providerFactory);

		AcceptHeaderByFileSuffixFilter suffixNegotiationFilter = new AcceptHeaderByFileSuffixFilter();
		suffixNegotiationFilter.setMediaTypeMappings(this.mediaTypes);
		suffixNegotiationFilter.setLanguageMappings(this.languages);
		providerFactory.getContainerRequestFilterRegistry().registerSingleton(suffixNegotiationFilter);

		for (Object bean : this.beans) {
			dispatcher.getRegistry().addSingletonResource(bean);
		}

		return new ResteasyFinagleService(dispatcher, executor);
	}

	public static ServiceBuilder get() {
		return new ServiceBuilder();
	}

}
