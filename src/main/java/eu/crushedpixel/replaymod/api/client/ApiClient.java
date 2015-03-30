package eu.crushedpixel.replaymod.api.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import eu.crushedpixel.replaymod.api.client.holders.ApiError;
import eu.crushedpixel.replaymod.api.client.holders.AuthKey;
import eu.crushedpixel.replaymod.api.client.holders.Donated;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;
import eu.crushedpixel.replaymod.api.client.holders.SearchResult;
import eu.crushedpixel.replaymod.api.client.holders.Success;
import eu.crushedpixel.replaymod.api.client.holders.UserFiles;
import eu.crushedpixel.replaymod.utils.StreamTools;

public class ApiClient {

	private static Gson gson = new Gson();
	private static JsonParser jsonParser = new JsonParser();

	public AuthKey getLogin(String username, String password) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.login);
		builder.put("user", username);
		builder.put("pw", password);
		AuthKey auth = invokeAndReturn(builder, AuthKey.class);
		return auth;
	}

	public boolean logout(String auth) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.logout);
		builder.put("auth", auth);
		Success succ = invokeAndReturn(builder, Success.class);
		return succ.isSuccess();
	}

	public boolean hasDonated(String uuid) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.check_auth);
		builder.put("uuid", uuid);
		Donated succ = invokeAndReturn(builder, Donated.class);
		return succ.hasDonated();
	}
	
	public UserFiles getUserFiles(String auth, String user) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.replay_files);
		builder.put("auth", auth);
		builder.put("user", user);
		UserFiles files = invokeAndReturn(builder, UserFiles.class);
		return files;
	}

	public FileInfo[] getFileInfo(String auth, List<Integer> ids) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.replay_files);
		builder.put("auth", auth);
		builder.put("ids", buildListString(ids));
		FileInfo[] info = invokeAndReturn(builder, FileInfo[].class);
		return info;
	}
	
	public FileInfo[] searchFiles(SearchQuery query) throws IOException, ApiException {
		StringBuilder sb = new StringBuilder();

		// build base url
		sb.append(QueryBuilder.API_BASE_URL);
		sb.append("search");
		sb.append(query.buildQuery());
		
		FileInfo[] info = invokeAndReturn(sb.toString(), SearchResult.class).getResults();
		return info;
	}
	
	public void downloadThumbnail(int file, File target) throws IOException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.get_thumbnail);
		builder.put("id", file);
		URL url = new URL(builder.toString());
		FileUtils.copyURLToFile(url, target);
	}

	public void downloadFile(String auth, int file, File target) throws IOException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.download_file);
		builder.put("auth", auth);
		builder.put("id", file);
		String url = builder.toString();
		URL website = new URL(url);
		HttpURLConnection con = (HttpURLConnection)website.openConnection();
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
			} catch(Exception e) {}
		}
	}

	public void rateFile(String auth, int file, boolean like) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.rate_file);
		builder.put("auth", auth);
		builder.put("id", file);
		builder.put("like", like);
		invokeAndReturn(builder, Success.class);
	}

	public void removeFile(String auth, int file) throws IOException, ApiException {
		QueryBuilder builder = new QueryBuilder(ApiMethods.remove_file);
		builder.put("auth", auth);
		builder.put("id", file);
		invokeAndReturn(builder, Success.class);
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
		Integer x=0;
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
