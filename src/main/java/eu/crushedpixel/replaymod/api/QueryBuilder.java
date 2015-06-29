package eu.crushedpixel.replaymod.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class QueryBuilder {

    public String apiMethod;
    public Map<String, String> paramMap;

    public QueryBuilder(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public void put(String key, Object value) {
        if(key != null && value != null) {
            if(paramMap == null) {
                paramMap = new HashMap<String, String>();
            }
            paramMap.put(key, value.toString());
        }
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
