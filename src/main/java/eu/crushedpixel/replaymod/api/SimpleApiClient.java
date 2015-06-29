package eu.crushedpixel.replaymod.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.crushedpixel.replaymod.api.replay.holders.ApiError;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class SimpleApiClient {

    private static final JsonParser jsonParser = new JsonParser();
    private static final Gson gson = new Gson();

    /**
     * Returns a Json String from the given QueryBuilder
     *
     * @param query The QueryBuilder to use
     * @return A Json String from the API
     * @throws IOException
     * @throws ApiException
     */
    public static String invoke(QueryBuilder query) throws IOException, ApiException {
        return invokeImpl(query.toString());
    }

    /**
     * Returns a Json String from the given URL
     *
     * @param url The URL to parse the Json from
     * @return A Json String from the API
     * @throws IOException
     * @throws ApiException
     */
    public static String invokeUrl(String url) throws IOException, ApiException {
        return invokeImpl(url);
    }

    /**
     * Returns a Json String from the API
     *
     * @param method   The apiMethod to be called
     * @param paramMap The parameters to apply
     * @return A Json String from the API
     * @throws IOException
     * @throws ApiException
     */
    public static String invoke(String method, Map<String, Object> paramMap) throws IOException, ApiException {
        return invokeImpl(method, paramMap);
    }

    /**
     * Returns a Json String from the API
     *
     * @param method The apiMethod to be called
     * @return A Json String from the API
     * @throws IOException
     * @throws ApiException
     */
    public static String invoke(String method) throws IOException, ApiException {
        return invokeImpl(method, null);
    }

    private static String invokeImpl(String urlString) throws IOException, ApiException {

        // read response
        String responseContent = null;
        InputStream is = null;
        HttpURLConnection httpUrlConnection = null;
        try {
            URL url = new URL(urlString);
            httpUrlConnection = (HttpURLConnection) url.openConnection();

            httpUrlConnection.setRequestMethod("GET");

            // give it 15 seconds to respond
            httpUrlConnection.setReadTimeout(15 * 1000);
            httpUrlConnection.connect();

            int responseCode = httpUrlConnection.getResponseCode();

            if(responseCode != 200) {
                is = httpUrlConnection.getErrorStream();
                if(is != null) {
                    responseContent = IOUtils.toString(is, "UTF-8");
                } else {
                    responseContent = "";
                }
                try {
                    JsonObject response = jsonParser.parse(responseContent).getAsJsonObject();
                    throw new ApiException(gson.fromJson(response, ApiError.class));
                } catch(JsonParseException e) {
                    throw new ApiException(responseContent);
                }
            }

            is = httpUrlConnection.getInputStream();

            responseContent = IOUtils.toString(is, "UTF-8");

        } finally {
            if(is != null) {
                is.close();
            }
            if(httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
        return responseContent;
    }

    private static String invokeImpl(String method, Map<String, Object> paramMap) throws IOException, ApiException {
        QueryBuilder queryBuilder = new QueryBuilder(method);
        queryBuilder.put(paramMap);
        return invokeImpl(queryBuilder.toString());
    }
}
