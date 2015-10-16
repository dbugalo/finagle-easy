package com.twitter.finagle.easy.server;

import com.google.common.collect.Sets;
import com.twitter.finagle.easy.server.NettyHeaderWrapper;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the wrapping of Netty message headers
 *
 * @author ed.peters
 */
public class TestNettyHeaderWrapper {

    private HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONFLICT);
    private NettyHeaderWrapper wrapper = new NettyHeaderWrapper(response);

    @Test
    public void testAdd() throws Exception {
        assertMapping("k");
        wrapper.add("k", "v1");
        assertMapping("k", "v1");
        wrapper.add("k", "v2");
        assertMapping("k", "v1", "v2");
    }

    @Test
    public void testPut() throws Exception {
        assertMapping("k");
        wrapper.putSingle("k", "v1");
        assertMapping("k", "v1");
        wrapper.putSingle("k", "v2");
        assertMapping("k", "v2");
        assertEquals(Arrays.asList("v2"),
                wrapper.put("k", Arrays.<Object>asList("v3", "v4")));
        assertMapping("k", "v3", "v4");
    }

    @Test
    public void testRemove() throws Exception {
        response.headers().set("k", "v");
        assertMapping("k", "v");
        assertEquals(Arrays.asList("v"), wrapper.remove("k"));
        assertMapping("k");
    }

    @Test
    public void testGetFirst() throws Exception {
        response.headers().add("k", "v1");
        response.headers().add("k", "v2");
        assertMapping("k", "v1", "v2");
        // weird semantics: "getFirst" actually means "get the most recently
        // added", which in this case is ...
        assertEquals("v1", response.headers().getAll("k").get(0));
        assertEquals("v2", response.headers().getAll("k").get(1));
    }

    @Test
    public void testKeySet() throws Exception {
        Set<String> ids = Sets.newHashSet();
        for (int i = 0; i < 10; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        for (String id : ids) {
            response.headers().set(id, "v-" + id);
        }
        assertEquals("response has wrong keys", ids, this.response.headers().names());
        assertEquals("wrapper has wrong keys", ids, this.wrapper.keySet());
    }

    @Test
    public void testEntrySet() throws Exception {
        Set<String> ids = Sets.newHashSet();
        for (int i = 0; i < 10; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        for (String id : ids) {
            response.headers().set(id, "v-" + id);
        }
        for (Map.Entry<String,String> entry : this.response.headers()) {
            assertTrue("unknown key", ids.contains(entry.getKey()));
            assertEquals("v-" + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String,List<Object>> entry : this.wrapper.entrySet()) {
            assertTrue("unknown key", ids.contains(entry.getKey()));
            assertEquals("v-" + entry.getKey(), entry.getValue().get(0));
        }
    }

    protected void assertMapping(String key, String... values) {
        List<String> expectedVals = Arrays.asList(values);
        assertEquals("underlying response has bad values for " + key,
                expectedVals,
                this.response.headers().getAll(key));
        assertEquals("wrapper reports bad values for " + key,
                expectedVals,
                this.wrapper.get(key));
    }

}
