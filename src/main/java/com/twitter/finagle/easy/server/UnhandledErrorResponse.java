package com.twitter.finagle.easy.server;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Provides a simple way to construct a Netty response message based on an
 * arbitrary exception.  Used when we're serving calls to a Resteasy
 * services and an exception occurs that isn't otherwise handled.
 *
 * NOTE: JAX-RS already specifies a mechanism that lets you customize how
 * service exceptions get converted into HTTP responses, and Resteasy has a
 * number of built-in renderings for common exception types.  So this should
 * only wind up getting used for strange exceptions that don't get handled
 * normally, like failures inside Resteasy itself.
 *
 * @author ed.peters
 * @author denis.rangel
 *
 * @see "http://docs.jboss.org/resteasy/2.0.0.GA/userguide/html/ExceptionHandling.html"
 */
public class UnhandledErrorResponse extends DefaultHttpResponse {

    public UnhandledErrorResponse(HttpVersion version, Exception error) {
        super(version, new HttpResponseStatus(500, error.toString()));
    }
}
