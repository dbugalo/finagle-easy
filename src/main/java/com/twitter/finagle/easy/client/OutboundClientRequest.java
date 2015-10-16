package com.twitter.finagle.easy.client;

import com.google.common.annotations.VisibleForTesting;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * Netty {@link org.jboss.netty.handler.codec.http.HttpRequest} implementation
 * that gets its data from a Resteasy {@link ClientRequest}.  Used when we're
 * making outbound calls via a Resteasy proxy, on the outbound leg.
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class OutboundClientRequest extends DefaultHttpRequest {

    /**
     * Wraps up the supplied Resteasy message in a Netty message.  This
     * requires us to get the Resteasy message to serialize itself (to
     * trigger processing by any registered Resteasy providers).
     *
     * @param resteasyRequest a Resteasy request
     * @throws Exception if something goes wrong
     */
    public OutboundClientRequest(ClientRequest resteasyRequest)
            throws Exception {

        super(
            HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(resteasyRequest.getHttpMethod()),
            stripProtocolAndHost(resteasyRequest.getUri()));

        // TODO this implementation will fully serialize the message
        // into memory -- obviously this sucks for large requests.
        // will the structure of the APIs allow us to do better?

        HeadersAndBody message = writeMessage(resteasyRequest);
        for (String name : message.headers.keySet()) {
        	headers().add(name, message.headers.get(name));
        }

        byte [] bytes = message.getBody().toByteArray();
        if (bytes.length == 0) {
        	headers().set(CONTENT_LENGTH, "0");
        } else {
        	headers().set(CONTENT_LENGTH, Integer.toString(bytes.length));
            setContent(ChannelBuffers.wrappedBuffer(bytes));
        }
    }

    /**
     * Strips off a preceding "protocol://host/" string, if there is one
     * at the beginning of the supplied URI.  Since we're going to dispatch
     * all our requests through Netty, we want relative URIs.
     *
     * @param uri a URI
     * @return the URI with leading protocol/host removed
     */
    @VisibleForTesting
    static String stripProtocolAndHost(String uri) {
        int idx = uri.indexOf("://");
        return idx >= 0
                ? uri.substring(uri.indexOf("/", idx + 3))
                : uri;
    }

    /**
     * Directs the supplied Resteasy message to serialize itself, and returns
     * the resulting body content and headers (writing the message might
     * trigger a JAX-RS provider which alters the original headers).
     *
     * @param resteasyRequest a Resteasy request
     * @return the resulting headers and message body
     * @throws java.io.IOException if something goes wrong
     */
    @VisibleForTesting
    static HeadersAndBody writeMessage(ClientRequest resteasyRequest)
            throws IOException {
        HeadersAndBody message = new HeadersAndBody();
        message.headers.putAll((MultivaluedMap) resteasyRequest.getHeaders());
        message.headers.remove(CONTENT_TYPE);
        message.setContentType(resteasyRequest.getBodyContentType());
        resteasyRequest.writeRequestBody(
                message.headers,
                message.getBody());
        return message;
    }

    /**
     * Represents an HTTP message that's been serialized.
     */
    @VisibleForTesting
    static class HeadersAndBody {

        private final MultivaluedMap<String,Object> headers;
        private final ByteArrayOutputStream bytes;

        public HeadersAndBody() {
            this.headers = new CaseInsensitiveMap<Object>();
            this.bytes = new ByteArrayOutputStream();
        }

        public ByteArrayOutputStream getBody() {
            return bytes;
        }

        public MultivaluedMap<String,Object> getHeaders() {
            return headers;
        }

        public void setContentType(MediaType contentType) {
            if (contentType != null) {
                headers.putSingle(CONTENT_TYPE, contentType.toString());
            }
        }
    }
}
