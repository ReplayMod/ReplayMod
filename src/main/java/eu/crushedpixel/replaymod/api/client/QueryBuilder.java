package eu.crushedpixel.replaymod.api.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class QueryBuilder {

	private static final String API_BASE_URL = "http://ReplayMod.com/api/";

	public String apiMethod;
	public Map<String,String> paramMap;

	/**
	 * Creates an empty QueryBuilder from a given apikey.
	 * <br>Note that in order to use the QueryBuilder an apiMethod String has to be set.
	 * @param apiKey The apikey to use
	 */
	public QueryBuilder() {
		this(null);
	}

	/**
	 * Creates an empty QueryBuilder from a given apikey and apiMethod.
	 * 
	 * @param apiKey The apikey to use
	 * @param apiMethod The apiMethod to use
	 */
	public QueryBuilder(String apiMethod) {
		this.apiMethod = apiMethod;
	}

	/**
	 * Creates a QueryBuilder from a given apikey and apiMethod containing a single key/value parameter.
	 * 
	 * @param apiKey The apikey to use
	 * @param apiMethod The apiMethod to use
	 * @param key The parameter key
	 * @param value The parameter value
	 */
	public QueryBuilder(String apiMethod, String key, String value) {
		this(apiMethod);
		put(key, value);
	}

	/**
	 * Creates a QueryBuilder from a given apikey and apiMethod containing two key/value parameters.
	 * 
	 * @param apiKey The apikey to use
	 * @param apiMethod The apiMethod to use
	 * @param key1 The first parameter key
	 * @param value1 The first parameter value
	 * @param key2 The second parameter key
	 * @param value2 The second parameter value
	 */
	public QueryBuilder(String apiMethod, String key1, String value1, String key2, String value2) {
		this(apiMethod);
		put(key1, value1);
		put(key2, value2);
	}

	/**
	 * Creates a QueryBuilder from a given apikey and apiMethod containing three key/value parameters.
	 * 
	 * @param apiKey The apikey to use
	 * @param apiMethod The apiMethod to use
	 * @param key1 The first parameter key
	 * @param value1 The first parameter value
	 * @param key2 The second parameter key
	 * @param value2 The second parameter value
	 * @param key3 The third parameter key
	 * @param value3 The third parameter value
	 */
	public QueryBuilder(String apiMethod, String key1, String value1, String key2, String value2, String key3, String value3) {
		this(apiMethod);
		put(key1, value1);
		put(key2, value2);
		put(key3, value3);
	}

	/**
	 * Adds a key/value parameter to the QueryBuilder.
	 * @param key The parameter key
	 * @param value The parameter value
	 */
	public void put(String key, Object value) {
		if(key != null && value != null) {
			if(paramMap == null) {
				paramMap = new HashMap<String,String>();
			}
			paramMap.put(key, value.toString());
		}
	}

	/**
	 * Adds two key/value parameters to the QueryBuilder.
	 * @param key1 The first parameter key
	 * @param value1 The first parameter value
	 * @param key2 The second parameter key
	 * @param value2 The second parameter value
	 */
	public void put(String key1, Object value1, String key2, Object value2) {
		put(key1, value1);
		put(key2, value2);
	}

	/**
	 * Adds three key/value parameters to the QueryBuilder.
	 * @param key1 The first parameter key
	 * @param value1 The first parameter value
	 * @param key2 The second parameter key
	 * @param value2 The second parameter value
	 * @param key3 The third parameter key
	 * @param value3 The third parameter value
	 */
	public void put(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
		put(key1, value1);
		put(key2, value2);
		put(key3, value3);
	}

	/**
	 * Adds a map of key/value parameters to the QueryBuilder. 
	 * @param paraMap The map to add 
	 */
	public void put(Map<String,Object> paraMap) {
		if(paraMap == null) return;
		for(String key: paraMap.keySet()) {
			put(key,paraMap.get(key));
		}
	}

	/**
	 * Creates an URL from the QueryBuilder using the given apikey and apiMethod and applies all
	 * parameters to it.
	 */
	public String toString() {
		if(apiMethod == null) throw new IllegalArgumentException("apiMethod may not be null");

		StringBuilder sb = new StringBuilder();

		// build base url
		sb.append(API_BASE_URL);
		sb.append(apiMethod);

		// process parameters
		try {
			if(paramMap != null) {
				boolean first = true;
				for(String paramName: paramMap.keySet()) {
					if(first) sb.append("?");
					if(!first) sb.append("&");
					first = false;
					sb.append(paramName);
					sb.append("=");
					String value = paramMap.get(paramName); 
					sb.append(URLEncoder.encode(value, "UTF-8"));
				}
			}
			return  sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
