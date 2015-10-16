package com.twitter.finagle.easy.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jboss.netty.handler.codec.http.HttpResponse;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the {@link javax.ws.rs.core.MultivaluedMap} API on top of
 * a Netty response object, so we can get Resteasy to write headers to
 * the correct location.
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class NettyHeaderWrapper implements MultivaluedMap<String, Object> {

    private final HttpResponse nettyResponse;

    public NettyHeaderWrapper(HttpResponse nettyResponse) {
        this.nettyResponse = nettyResponse;
    }

    @Override
    public void add(String key, Object value) {
        nettyResponse.headers().add(key, value);
    }

    @Override
    public void putSingle(String key, Object value) {
        nettyResponse.headers().set(key, value);
    }

    @Override
    public Object getFirst(String key) {
        return nettyResponse.headers().getAll(key).get(0);
    }

    @Override
    public int size() {
        return nettyResponse.headers().entries().size();
    }

    @Override
    public boolean isEmpty() {
        return size() > 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return nettyResponse.headers().contains(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("containsValue");
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Object> get(Object key) {
        // OK because we're returning a List
        return (List) nettyResponse.headers().getAll(key.toString());
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Object> put(String key, List<Object> values) {
        // this is safe -- we're converting List<String> to List<Object>
        List<Object> oldValue = (List) nettyResponse.headers().getAll(key.toString());
        nettyResponse.headers().set(key, values);
        return oldValue;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Object> remove(Object key) {
        // this is safe -- we're converting List<String> to List<Object>
        List<Object> oldValue = (List) nettyResponse.headers().getAll(key.toString());
        nettyResponse.headers().remove(key.toString());
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<Object>> map) {
        for (String key : map.keySet()) {
            put(key, map.get(key));
        }
    }

    @Override
    public void clear() {
        nettyResponse.headers().clear();
    }

    @Override
    public Set<String> keySet() {
        return nettyResponse.headers().names();
    }

    @Override
    public Collection<List<Object>> values() {
        List<List<Object>> all = Lists.newArrayList();
        for (String key : keySet()) {
            all.add(get(key));
        }
        return all;
    }

    @Override
    public Set<Entry<String,List<Object>>> entrySet() {
        Map<String,List<Object>> map = Maps.newHashMap();
        for (String key : keySet()) {
            map.put(key, get(key));
        }
        return map.entrySet();
    }

	@Override
	public void addAll(String arg0, Object... arg1) {
		this.addAll(arg0, Arrays.asList(arg1));
	}

	@Override
	public void addAll(String arg0, List<Object> arg1) {
		nettyResponse.headers().add(arg0, arg1);
	}

	@Override
	public void addFirst(String arg0, Object arg1) {
		nettyResponse.headers().add(arg0, arg1);
	}

	@Override
	public boolean equalsIgnoreValueOrder(MultivaluedMap<String, Object> arg0) {
		// TODO Auto-generated method stub
		return false;
	}
}
