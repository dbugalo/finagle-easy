package com.twitter.finagle.easy.util;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Helpers for converting headers and URIs between their corresponding Netty
 * and Resteasy (JAX-RS) structures.
 *
 * @author ed.peters
 */
public final class ServiceUtils {

    /**
     * Converts String values (used by Netty) to MediaTypes (used by JAX-RS)
     */
    public static final Function<String,MediaType> TO_MEDIA_TYPE =
        new Function<String, MediaType>() {
            @Override
            public MediaType apply(@Nullable String input) {
                return input == null ? null : MediaType.valueOf(input);
            }
        };

    /**
     * Splits a header string on the "," character
     */
    public static final Function<String, List<String>> SPLIT_HEADER_VALUES =
        new Function<String, List<String>>() {
            @Override
            public List<String> apply(@Nullable String input) {
                return input == null 
                    ? Lists.<String>newArrayList() 
                    : Lists.newArrayList(input.split(","));
            }
        };

    /**
     * Base URI for all requests ("/")
     */
    public static final URI SLASH;
    static {
        try {
            SLASH = new URI("/");
        }
        catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    private ServiceUtils() {}

    /**
     * Creates a new {@link MultivaluedMap} from the supplied data.
     *
     * @param data key/value pairs, where the values are either single Strings
     *             or Lists of Strings
     * @return the resulting map
     */
    @SuppressWarnings("unchecked")
	public static MultivaluedMap<String, String> newMultiValuedMap(Object... data) {
        MultivaluedMap<String,String> map = new CaseInsensitiveMap<String>();
        for (int i = 0; i < data.length; i+=2) {
            String key = data[i].toString();
            Object val = data[i + 1];
            if (val instanceof List) {
                map.put(key, (List<String>)val);
            }
            else {
                map.putSingle(key, val.toString());
            }
        }
        return map;
    }

    /**
     * @return the headers from the supplied Netty message, as a JAX-RS
     * {@link MultivaluedMap} (the returned map is never null, and is
     * case-insensitive)
     */
    public static MultivaluedMap<String,String> toMultimap(HttpMessage message) {
        MultivaluedMap<String, String> map = new CaseInsensitiveMap<String>();
        for (String name : message.headers().names()) {
            map.put(name, message.headers().getAll(name));
        }
        return map;
    }

    /**
     * @return the headers from the supplied Resteasy {@link ClientRequest},
     * as a JAX-RS {@link MultivaluedMap} (the returned map is never null,
     * is case-insenstivie, and is a mutable copy of what's currently in
     * the request)
     */
    public static MultivaluedMap<String,String> toMultimap(ClientRequest request) {
        // making a copy of the headers that were passed in, in case they're
        // immutable (the cast is safe: we're converting Multimap<String,String>
        // to Multimap<String,Object>)
        MultivaluedMap<String,String> map = new CaseInsensitiveMap<String>();
        map.putAll((MultivaluedMap) request.getHeaders());
        if (request.getBodyContentType() != null) {
            map.putSingle(
                    CONTENT_TYPE,
                    request.getBodyContentType().toString());
        }
        return map;
    }

    /**
     * @return the contents of the supplied map, as JAX-RS
     * {@link javax.ws.rs.core.HttpHeaders}
     */
    public static HttpHeaders toHeaders(MultivaluedMap<String,String> map) {
    	return new ResteasyHttpHeaders(map);
    }

    /**
     * @param nettyRequest a request from Netty
     * @return a {@link javax.ws.rs.core.UriInfo} object for the specified URI
     */
    public static UriInfo toUriInfo(HttpRequest nettyRequest) {
        String host = "localhost";
        int port = 80;
        String hostHeader = nettyRequest.headers().get(HOST);
        if (hostHeader != null) {
            try {
                String [] split = hostHeader.split(":");
                host = split[0];
                if (split.length > 1) {
                    port = Integer.parseInt(split[1]);
                }
            }
            catch (Exception e) {
                throw new IllegalArgumentException(
                        "bad host header: " + hostHeader, e);
            }
        }
        // TODO how can we determine whether the request was SSL or not?
        return toUriInfo(nettyRequest.getUri(), host, port, false);
    }

    /**
     * @param uri a request URI, as extracted from a Netty request
     * @param host the original host for the request
     * @param port the original port for the request
     * @param secure did the original request come in on a secure channel?
     * @return a {@link javax.ws.rs.core.UriInfo} object for the specified URI
     */
    public static UriInfo toUriInfo(String uri,
                                    String host,
                                    int port,
                                    boolean secure) {

        // this was ripped off from JBoss code that does the same thing
        // starting with an HttpServletRequest
        // @see org.jboss.resteasy.plugins.server.servlet.ServletUtil#extractUriInfo()

        String [] split = uri.split("\\?", 2);
        String path = split[0];
        String query = null;
        if (split.length > 1) {
            query = split[1];
        }

        URI absolutePath = new ResteasyUriBuilder()
                .scheme(secure ? "https" : "http")
                .host(host == null ? "localhost" : host)
                .port(port)
                .path(path)
                .replaceQuery(query)
                .build();

        return new ResteasyUriInfo(absolutePath);
    }

    /**
     * Gets the default instance of the ResteasyProviderFactory and manually
     * prods it to register built-in providers by scanning the classpath.
     */
    public static ResteasyProviderFactory getDefaultProviderFactory() {
        ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(factory);
        return factory;
    }

}
