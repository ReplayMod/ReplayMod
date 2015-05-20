package eu.crushedpixel.replaymod.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class QueryBuilder {

    public String apiMethod;
    public Map<String, String> paramMap;

    public QueryBuilder() {
        this(null);
    }

    public QueryBuilder(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public QueryBuilder(String apiMethod, String key, String value) {
        this(apiMethod);
        put(key, value);
    }

    public QueryBuilder(String apiMethod, String key1, String value1, String key2, String value2) {
        this(apiMethod);
        put(key1, value1);
        put(key2, value2);
    }

    public QueryBuilder(String apiMethod, String key1, String value1, String key2, String value2, String key3, String value3) {
        this(apiMethod);
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
    }

    public void put(String key, Object value) {
        if(key != null && value != null) {
            if(paramMap == null) {
                paramMap = new HashMap<String, String>();
            }
            paramMap.put(key, value.toString());
        }
    }

    public void put(String key1, Object value1, String key2, Object value2) {
        put(key1, value1);
        put(key2, value2);
    }

    public void put(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
    }

    public void put(Map<String, Object> paraMap) {
        if(paraMap == null) return;
        for(String key : paraMap.keySet()) {
            put(key, paraMap.get(key));
        }
    }

    public String toString() {
        if(apiMethod == null) throw new IllegalArgumentException("apiMethod may not be null");

        StringBuilder sb = new StringBuilder();

        // build base url
        sb.append(apiMethod);

        // process parameters
        try {
            if(paramMap != null) {
                boolean first = true;
                for(String paramName : paramMap.keySet()) {
                    if(first) sb.append("?");
                    if(!first) sb.append("&");
                    first = false;
                    sb.append(paramName);
                    sb.append("=");
                    String value = paramMap.get(paramName);
                    sb.append(URLEncoder.encode(value, "UTF-8"));
                }
            }
            return sb.toString();
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
