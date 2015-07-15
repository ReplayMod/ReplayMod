package eu.crushedpixel.replaymod.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import eu.crushedpixel.replaymod.api.replay.ReplayModApiMethods;
import eu.crushedpixel.replaymod.api.replay.SearchQuery;
import eu.crushedpixel.replaymod.api.replay.holders.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHash;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ApiClient {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    public AuthKey getLogin(String username, String password) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.login);
        builder.put("user", username);
        builder.put("pw", password);
        builder.put("mod", true);
        return invokeAndReturn(builder, AuthKey.class);
    }

    public AuthKey register(String username, String mail, String password, String uuid)
            throws IOException, ApiException, AuthenticationException {

        AuthenticationHash authenticationHash = sessionserverJoin();

        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.register);
        builder.put("username", username);
        builder.put("email", mail);
        builder.put("password", password);
        builder.put("uuid", uuid);
        builder.put("mcusername", authenticationHash.username);
        builder.put("timelong", authenticationHash.currentTime);
        builder.put("randomlong", authenticationHash.randomLong);
        return invokeAndReturn(builder, AuthKey.class);
    }

    private AuthenticationHash sessionserverJoin() throws AuthenticationException {
        AuthenticationHash hash = new AuthenticationHash();

        mc.getSessionService().joinServer(
                mc.getSession().getProfile(), mc.getSession().getToken(), hash.hash);

        return hash;
    }

    public AuthConfirmation checkAuthkey(String auth) {
        try {
            QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.check_authkey);
            builder.put("auth", auth);
            return invokeAndReturn(builder, AuthConfirmation.class);
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

    public FileInfo[] getFileInfo(List<Integer> ids) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.file_details);
        builder.put("id", buildListString(ids));
        return invokeAndReturn(builder, FileInfo[].class);
    }

    public FileInfo[] searchFiles(SearchQuery query) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.search);
        return invokeAndReturn(builder.toString() + query.buildQuery(), SearchResult.class).getResults();
    }

    public String getTranslation(String languageCode) throws IOException, ApiException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_language);
        builder.put("language", languageCode);
        return SimpleApiClient.invokeUrl(builder.toString());
    }

    public void downloadThumbnail(int file, File target) throws IOException {
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.get_thumbnail);
        builder.put("id", file);
        URL url = new URL(builder.toString());
        FileUtils.copyURLToFile(url, target);
    }

    private boolean cancelDownload = false;

    public void downloadFile(String auth, int file, File target, ProgressUpdateListener listener) throws IOException, ApiException {
        cancelDownload = false;

        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.download_file);
        builder.put("auth", auth);
        builder.put("id", file);
        String url = builder.toString();
        URL website = new URL(url);
        HttpURLConnection con = (HttpURLConnection) website.openConnection();

        int fileSize = con.getContentLength();

        InputStream is = con.getInputStream();

        if(con.getResponseCode() == 200) {
            BufferedInputStream bin = new BufferedInputStream(is);
            FileOutputStream fout = new FileOutputStream(target);
            try {
                final byte data[] = new byte[1024];
                int count;
                int read = 0;
                while ((count = bin.read(data, 0, 1024)) != -1) {
                    if(cancelDownload) {
                        bin.close();
                        fout.close();

                        FileUtils.deleteQuietly(target);

                        return;
                    }

                    fout.write(data, 0, count);
                    read += count;
                    listener.onProgressChanged((float)(read)/fileSize);
                }
            } finally {
                bin.close();
                fout.close();
            }
        } else {
            JsonElement element = jsonParser.parse(IOUtils.toString(is));
            try {
                ApiError err = gson.fromJson(element, ApiError.class);
                if(err.getDesc() != null) {
                    throw new ApiException(err);
                }
            } catch(JsonParseException e) {
                throw new ApiException(IOUtils.toString(is));
            }
        }
    }

    public void cancelDownload() {
        this.cancelDownload = true;
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

    public boolean isVersionUpToDate(String versionIdentifier) throws IOException, ApiException {
        //in a development environment, getContainer().getVersion() will return ${version}
        if(versionIdentifier.equals("${version}")) return true;
        QueryBuilder builder = new QueryBuilder(ReplayModApiMethods.up_to_date);
        builder.put("version", versionIdentifier);
        return invokeAndReturn(builder, Success.class).isSuccess();
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
