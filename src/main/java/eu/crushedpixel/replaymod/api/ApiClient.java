package eu.crushedpixel.replaymod.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.crushedpixel.replaymod.api.mojang.MojangApiMethods;
import eu.crushedpixel.replaymod.api.mojang.holders.Profile;
import eu.crushedpixel.replaymod.api.replay.ReplayModApiMethods;
import eu.crushedpixel.replaymod.api.replay.SearchQuery;
import eu.crushedpixel.replaymod.api.replay.holders.*;
import eu.crushedpixel.replaymod.utils.StreamTools;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class ApiClient {

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    public AuthKey getLogin(String username, String password) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.login);
        builder.put("user", username);
        builder.put("pw", password);
        builder.put("mod", true);
        AuthKey auth = invokeAndReturn(builder, AuthKey.class);
        return auth;
    }

    public AuthKey register(String username, String mail, String password, String uuid, String accessToken)
            throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.register);
        builder.put("username", username);
        builder.put("email", mail);
        builder.put("password", password);
        builder.put("uuid", uuid);
        builder.put("accesstoken", accessToken);
        AuthKey auth = invokeAndReturn(builder, AuthKey.class);
        return auth;
    }

    public AuthConfirmation checkAuthkey(String auth) {
        try {
            QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.check_authkey);
            builder.put("auth", auth);
            AuthConfirmation conf = invokeAndReturn(builder, AuthConfirmation.class);
            return conf;
        } catch(Exception e) {
            return null;
        }
    }

    public boolean logout(String auth) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.logout);
        builder.put("auth", auth);
        builder.put("mod", true);
        Success succ = invokeAndReturn(builder, Success.class);
        return succ.isSuccess();
    }

    public boolean hasDonated(String uuid) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.check_auth);
        builder.put("uuid", uuid);
        Donated succ = invokeAndReturn(builder, Donated.class);
        return succ.hasDonated();
    }

    @Deprecated
    public UserFiles getUserFiles(String auth, String user) throws IOException, ApiException {
        //TODO if required
        return null;
    }

    public FileInfo[] getFileInfo(List<Integer> ids) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.file_details);
        builder.put("id", buildListString(ids));
        FileInfo[] info = invokeAndReturn(builder, FileInfo[].class);
        return info;
    }

    public FileInfo[] searchFiles(SearchQuery query) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.search);
        StringBuilder sb = new StringBuilder(builder.toString());
        sb.append(query.buildQuery());

        FileInfo[] info = invokeAndReturn(sb.toString(), SearchResult.class).getResults();
        return info;
    }

    public String getTranslation(String languageCode) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_language);
        builder.put("language", languageCode);
        String properties = SimpleApiClient.invokeUrl(builder.toString());
        return properties;
    }

    public void downloadThumbnail(int file, File target) throws IOException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_thumbnail);
        builder.put("id", file);
        URL url = new URL(builder.toString());
        FileUtils.copyURLToFile(url, target);
    }

    public void downloadFile(String auth, int file, File target) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.download_file);
        builder.put("auth", auth);
        builder.put("id", file);
        String url = builder.toString();
        URL website = new URL(url);
        HttpURLConnection con = (HttpURLConnection) website.openConnection();
        InputStream is = con.getInputStream();

        if(con.getResponseCode() == 200) {
            Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            JsonElement element = jsonParser.parse(StreamTools.readStreamtoString(is));
            try {
                ApiError err = gson.fromJson(element, ApiError.class);
                if(err.getDesc() != null) {
                    throw new ApiException(err);
                }
            } catch(JsonParseException e) {
                throw new ApiException(StreamTools.readStreamtoString(is));
            }
        }
    }

    public void rateFile(String auth, int file, Rating.RatingType rating) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.rate_file);
        builder.put("auth", auth);
        builder.put("id", file);
        builder.put("rating", rating.getKey());
        invokeAndReturn(builder, Success.class);
    }

    public FileRating[] getRatedFiles(String auth) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_ratings);
        builder.put("auth", auth);

        return invokeAndReturn(builder, RatedFiles.class).getRated();
    }

    public void favFile(String auth, int file, boolean fav) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.fav_file);
        builder.put("auth", auth);
        builder.put("id", file);
        builder.put("fav", fav);
        invokeAndReturn(builder, Success.class);
    }

    public int[] getFavorites(String auth) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_favorites);
        builder.put("auth", auth);

        return invokeAndReturn(builder, Favorites.class).getFavorited();
    }

    public void removeFile(String auth, int file) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.remove_file);
        builder.put("auth", auth);
        builder.put("id", file);
        invokeAndReturn(builder, Success.class);
    }

    /*
     MOJANG API CALLS
    */

    public Profile getProfileFromUUID(UUID uuid) throws IOException, ApiException {
        String url = String.format(MojangApiMethods.userprofile+"%s", uuid.toString().replace("-", ""));
        return invokeAndReturn(url, Profile.class);
    }

    private <T> T invokeAndReturn(QueryBuilder builder, Class<T> classOfT) throws IOException, ApiException {
        return invokeAndReturn(builder.toString(), classOfT);
    }

    private <T> T invokeAndReturn(String url, Class<T> classOfT) throws IOException, ApiException {
        JsonElement ele = GsonApiClient.invokeJson(url);
        return gson.fromJson(ele, classOfT);
    }

    @SuppressWarnings("rawtypes")
    private String buildListString(List idList) {
        if(idList == null) return null;

        String ids = "";
        Integer x = 0;
        for(Object id : idList) {
            x++;
            ids += id.toString();
            if(x != idList.size()) {
                ids += ",";
            }
        }

        return ids;
    }
}
