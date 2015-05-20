package eu.crushedpixel.replaymod.api.replay;

import com.google.gson.Gson;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.api.replay.holders.ApiError;
import eu.crushedpixel.replaymod.api.replay.holders.Category;
import eu.crushedpixel.replaymod.gui.online.GuiUploadFile;
import net.minecraft.client.resources.I18n;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

public class FileUploader {
    private static final Gson gson = new Gson();

    private boolean uploading = false;
    private long filesize;
    private long current;

    private String attachmentName = "file";
    private String attachmentFileName = "file.mcpr";
    private String crlf = "\r\n";
    private String twoHyphens = "--";

    private boolean cancel = false;

    private String boundary = "*****";
    private GuiUploadFile parent;

    public void uploadFile(GuiUploadFile gui, String auth, String filename, List<String> tags, File file, Category category) throws IOException, ApiException, RuntimeException {
        boolean success = false;
        String info = null;

        try {
            parent = gui;
            gui.onStartUploading();
            filesize = 0;

            if(uploading) throw new RuntimeException("FileUploader is already uploading");
            uploading = true;

            String postData = "?auth=" + auth + "&category=" + category.getId();

            if(tags.size() > 0) {
                postData += "&tags=";
                for(String tag : tags) {
                    postData += tag;
                    if(!tag.equals(tags.get(tags.size() - 1))) {
                        postData += ",";
                    }
                }
            }

            postData += "&name=" + URLEncoder.encode(filename, "UTF-8");
            System.out.println(postData);

            String url = "http://ReplayMod.com/api/upload_file" + postData;
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setChunkedStreamingMode(1024);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);

            HashMap<String, String> params = new HashMap<String, String>();
            params.put("auth", auth);
            params.put("name", filename);
            params.put("category", category.getId() + "");

            DataOutputStream request = new DataOutputStream(con.getOutputStream());

            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" + this.attachmentName + "\";filename=\"" + this.attachmentFileName + "\"" + this.crlf);
            request.writeBytes(this.crlf);

            byte[] buf = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            filesize = fis.getChannel().size();
            current = 0;
            int len;
            while((len = fis.read(buf)) != -1) {
                request.write(buf);
                current += len;
                if(cancel) {
                    uploading = false;
                    current = 0;
                    cancel = false;
                    parent.onFinishUploading(false, I18n.format("replaymod.gui.upload.canceled"));
                    fis.close();
                    return;
                }
            }
            fis.close();

            request.writeBytes(this.crlf);
            request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);

            request.flush();
            request.close();

            success = false;
            int responseCode = con.getResponseCode();
            InputStream is;
            if(responseCode == 200) {
                success = true;
                is = con.getInputStream();
            } else {
                is = con.getErrorStream();
            }

            if(is != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                info = null;
                if(responseCode != 200) {
                    String json = "";
                    while(r.ready()) {
                        json += r.readLine();
                    }
                    ApiError error = gson.fromJson(json, ApiError.class);
                    info = error.getTranslatedDesc();
                    System.out.println(info);
                }
            }
            con.disconnect();

            if(info == null) info = I18n.format("replaymod.gui.unknownerror");

            ReplayMod.uploadedFileHandler.markAsUploaded(file);

            success = true;
        } catch(Exception e) {
            success = false;
        } finally {
            parent.onFinishUploading(success, info);

            uploading = false;
        }
    }

    public float getUploadProgress() {
        if(!uploading || filesize == 0) return 0;
        return (float) ((double) current / (double) filesize);
    }

    public boolean isUploading() {
        return uploading;
    }

    public void cancelUploading() {
        cancel = true;
    }
}
