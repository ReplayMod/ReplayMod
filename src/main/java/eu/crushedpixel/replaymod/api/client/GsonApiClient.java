package eu.crushedpixel.replaymod.api.client;

import java.io.IOException;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GsonApiClient {	

	private static final JsonParser parser = new JsonParser();
	
	public static JsonObject invoke(QueryBuilder query) throws IOException, ApiException {
		String apiResult = SimpleApiClient.invoke(query);
		return wrapWithJson(apiResult);
	}

	public static JsonObject invokeJson(String url) throws IOException, ApiException {
		String apiResult =  SimpleApiClient.invokeUrl(url);
		return wrapWithJson(apiResult);
	}

	public static JsonObject invokeJson(String apiKey, String method, Map<String,Object> paramMap) throws IOException, ApiException {
		String apiResult =  SimpleApiClient.invoke(method, paramMap);
		return wrapWithJson(apiResult);
	}

	public static JsonObject invokeJson(String apiKey, String method) throws IOException, ApiException {
		String apiResult =  SimpleApiClient.invoke(method, null);
		return wrapWithJson(apiResult);
	}	
	
	private static JsonObject wrapWithJson(String apiResult) {
		JsonElement element = parser.parse(apiResult);
		return element.getAsJsonObject();
	}
}
