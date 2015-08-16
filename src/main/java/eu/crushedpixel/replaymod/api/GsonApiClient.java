package eu.crushedpixel.replaymod.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;

public class GsonApiClient {

    private static final JsonParser parser = new JsonParser();

    public static JsonElement invoke(QueryBuilder query) throws IOException, ApiException {
        String apiResult = SimpleApiClient.invoke(query);
        return wrapWithJson(apiResult);
    }

    public static JsonElement invokeJson(String url) throws IOException, ApiException {
        String apiResult = StringEscapeUtils.unescapeHtml4(SimpleApiClient.invokeUrl(url).replace("&#34;", "\\\""));
        return wrapWithJson(apiResult);
    }

    private static JsonElement wrapWithJson(String apiResult) {
        return parser.parse(apiResult);
    }
}
