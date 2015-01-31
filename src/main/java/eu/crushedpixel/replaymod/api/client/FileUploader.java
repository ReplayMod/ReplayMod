package eu.crushedpixel.replaymod.api.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import eu.crushedpixel.replaymod.api.client.holders.Category;

public class FileUploader {
	private static Gson gson = new Gson();
	private static JsonParser jsonParser = new JsonParser();

	private boolean uploading = false;
	private long filesize;
	private long current;
	
	private String attachmentName = "file";
	private String attachmentFileName = "file.mcpr";
	private String crlf = "\r\n";
	private String twoHyphens = "--";

	private String boundary = "*****";
	//private CountingHttpEntity counter;

	public void uploadFile(String auth, String filename, File file, Category category) throws IOException, ApiException, RuntimeException {
		if(uploading) throw new RuntimeException("FileUploader is already uploading");
		uploading = true;

		filesize = file.length();
		current = 0;
		
		String postData = "?auth="+auth+"&category="+category.getId()+"&name="+filename;
		
		String url = "http://ReplayMod.com/api/upload_file"+postData;
		HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
		con.setUseCaches(false);
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Connection", "Keep-Alive");
		con.setRequestProperty("Cache-Control", "no-cache");
		con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("auth", auth);
		params.put("name", filename);
		params.put("category", category.getId()+"");

		DataOutputStream request = new DataOutputStream(con.getOutputStream());

		request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
		request.writeBytes("Content-Disposition: form-data; name=\"" + this.attachmentName + "\";filename=\"" + this.attachmentFileName + "\"" + this.crlf);
		request.writeBytes(this.crlf);
		
		byte[] buf = new byte[1024];
		FileInputStream fis = new FileInputStream(file);
		int len;
		while((len = fis.read(buf)) != -1) {
			request.write(buf);
			current += len;
		}
		fis.close();
		
		request.writeBytes(this.crlf);
		request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);
		
		request.flush();
		request.close();
		
		int responseCode = ((HttpURLConnection)con).getResponseCode();
		InputStream is = null;
		if(responseCode == 200) {
			is = con.getInputStream();
		} else {
			is = con.getErrorStream();
		}
		
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		
		while(r.ready()) {
			System.out.println(r.readLine());
		}
		
		System.out.println(responseCode); // Should be 200

		con.disconnect();
		
		uploading = false;
	}

	public float getUploadProgress() {
		if(!uploading) return 0.1f;
		return (float)((double)current/(double)filesize);
	}
}
