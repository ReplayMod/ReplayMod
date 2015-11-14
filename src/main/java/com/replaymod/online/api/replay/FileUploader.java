package com.replaymod.online.api.replay;

import com.google.gson.Gson;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.replay.holders.ApiError;
import com.replaymod.online.api.replay.holders.Category;
import com.replaymod.online.gui.GuiUploadFile;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.resources.I18n;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

@RequiredArgsConstructor
public class FileUploader {
    private static final Gson gson = new Gson();

    private final ApiClient apiClient;

    private boolean uploading = false;
    private long filesize;
    private long current;

    private boolean cancel = false;

    private GuiUploadFile parent;

    public void uploadFile(GuiUploadFile gui, String filename, List<String> tags, File file, Category category, String description) {
        boolean success = false;
        String info = null;

        try {
            parent = gui;
            gui.onStartUploading();
            filesize = 0;

            if(uploading) throw new RuntimeException("FileUploader is already uploading");
            uploading = true;

            String postData = "?auth=" + apiClient.getAuthKey() + "&category=" + category.getId();

            if(tags.size() > 0) {
                postData += "&tags=";
                for(String tag : tags) {
                    postData += tag;
                    if(!tag.equals(tags.get(tags.size() - 1))) {
                        postData += ",";
                    }
                }
            }

            if(description != null && description.length() > 0) {
                postData += "&description=" + URLEncoder.encode(description, "UTF-8");
            }

            postData += "&name=" + URLEncoder.encode(filename, "UTF-8");

            String url = ReplayModApiMethods.upload_file + postData;
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setChunkedStreamingMode(1024);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Cache-Control", "no-cache");
            String boundary = "*****";
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(con.getOutputStream());

            String crlf = "\r\n";
            String twoHyphens = "--";
            request.writeBytes(twoHyphens + boundary + crlf);
            String attachmentName = "file";
            String attachmentFileName = "file.mcpr";
            request.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\"" + crlf);
            request.writeBytes(crlf);

            byte[] buf = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            filesize = fis.getChannel().size();
            current = 0;
            int len;
            while((len = fis.read(buf)) != -1) {
                request.write(buf);
                current += len;

                parent.onProgressChanged(getUploadProgress());

                if(cancel) {
                    parent.onProgressChanged(0f);

                    uploading = false;
                    current = 0;
                    cancel = false;
                    parent.onFinishUploading(false, I18n.format("replaymod.gui.upload.canceled"));
                    fis.close();
                    return;
                }
            }
            fis.close();

            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            request.flush();
            request.close();

            success = false;
            int responseCode = con.getResponseCode();
            InputStream is;
            if(responseCode == 200) {
                success = true;
                is = con.getInputStream();
            } else {
                success = false;
                is = con.getErrorStream();
            }

            if(is != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                info = null;
                String result = "";
                while(r.ready()) {
                    result += r.readLine();
                }
                if(responseCode != 200) {
                    ApiError error = gson.fromJson(result, ApiError.class);
                    info = error.getTranslatedDesc();
                }
            }
            con.disconnect();

            if(info == null) info = I18n.format("replaymod.gui.unknownerror");
        } catch(Exception e) {
            success = false;
            e.printStackTrace();
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
