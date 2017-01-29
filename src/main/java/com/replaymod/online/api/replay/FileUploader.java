package com.replaymod.online.api.replay;

import com.google.gson.Gson;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.ApiException;
import com.replaymod.online.api.replay.holders.ApiError;
import com.replaymod.online.api.replay.holders.Category;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import java.util.function.Consumer;

import static com.replaymod.core.utils.Utils.SSL_SOCKET_FACTORY;

@RequiredArgsConstructor
public class FileUploader {
    private static final Gson gson = new Gson();

    private final ApiClient apiClient;

    private volatile boolean uploading = false;
    private volatile boolean cancel;
    private long filesize;
    private long current;

    public synchronized void uploadFile(File file, String filename, Set<String> tags, Category category,
                                        String description, Consumer<Double> progress) throws Exception {
        try {
            uploading = true;

            String postData = "?auth=" + apiClient.getAuthKey() + "&category=" + category.getId();

            if(tags.size() > 0) {
                postData += "&tags=" + StringUtils.join(tags.toArray(new String[tags.size()]), ",");
            }

            if(description != null && description.length() > 0) {
                postData += "&description=" + URLEncoder.encode(description, "UTF-8");
            }

            postData += "&name=" + URLEncoder.encode(filename, "UTF-8");

            String url = ReplayModApiMethods.upload_file + postData;
            HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
            con.setSSLSocketFactory(SSL_SOCKET_FACTORY);
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

                progress.accept(getUploadProgress());

                if(cancel) {
                    fis.close();
                    throw new CancelledException();
                }
            }
            fis.close();

            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            request.flush();
            request.close();

            int responseCode = con.getResponseCode();
            InputStream is = responseCode == 200 ? con.getInputStream() : con.getErrorStream();
            if (is == null) {
                throw new RuntimeException("Input stream was null.");
            }

            String result = IOUtils.toString(is);
            if (responseCode != 200) {
                ApiError error = gson.fromJson(result, ApiError.class);
                throw new ApiException(error);
            }
            con.disconnect();

        } finally {
            uploading = false;
            cancel = false;
            current = 0;
        }
    }

    public double getUploadProgress() {
        if(!uploading || filesize == 0) return 0;
        return (double) current / filesize;
    }

    public void cancelUploading() {
        cancel = true;
    }

    public static final class CancelledException extends Exception {}
}
