package com.replaymod.extras.youtube;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.metadata.MetadataInjector;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class YoutubeUploader {
    private static final String CLIENT_ID = "743126594724-mfe7pj1k7e47uu5pk4503c8st9vj9ibu.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "gMwcy3mRYCRamCIjJIYP7rqc";
    private static final String FFMPEG_ODS =
            "-i %s -vf scale=iw:iw*9/16,setdar=16:9 -c:v libx264 -preset slow -crf 16 %s";
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private final NetHttpTransport httpTransport;
    private final DataStoreFactory dataStoreFactory;
    private final File videoFile;
    private final int videoFrames;
    private final String thumbnailFormat;
    private final byte[] thumbnailImage;
    private final RenderSettings settings;

    private final VideoSnippet videoSnippet;
    private final VideoVisibility videoVisibility;

    private Thread thread;

    @NonNull
    private Supplier<Double> progress = Suppliers.ofInstance(0d);

    @Getter
    private State state;

    @Getter
    private volatile boolean cancelled;

    public YoutubeUploader(Minecraft minecraft, File videoFile, int videoFrames,
                           String thumbnailFormat, byte[] thumbnailImage,
                           RenderSettings settings, VideoVisibility videoVisibility, VideoSnippet videoSnippet)
            throws GeneralSecurityException, IOException {
        this.videoFile = videoFile;
        this.videoFrames = videoFrames;
        this.thumbnailImage = thumbnailImage;
        this.thumbnailFormat = thumbnailFormat;
        this.settings = settings;
        this.videoVisibility = videoVisibility;
        this.videoSnippet = videoSnippet;
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.dataStoreFactory = new FileDataStoreFactory(minecraft.mcDataDir);

    }

    public ListenableFuture<Video> upload() throws IOException {
        cancelled = false;

        final SettableFuture<Video> future = SettableFuture.create();
        thread = new Thread(() -> {
            try {
                state = State.AUTH;
                Credential credential = auth();

                state = State.PREPARE_VIDEO;
                File processedFile = preUpload();
                progress = Suppliers.ofInstance(0d);

                YouTube youTube = new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                        .setApplicationName("ReplayMod").build();

                state = State.UPLOAD;
                Video video = doUpload(youTube, processedFile);
                if (thumbnailImage != null) {
                    doThumbUpload(youTube, video);
                }

                state = State.CLEANUP;
                postUpload(processedFile);
                future.set(video);
            } catch (Throwable t) {
                future.setException(t);
            }
        });
        thread.start();
        return future;
    }

    //I blame the Google SDK for not supporting "proper" upload cancellation
    @SuppressWarnings("unchecked")
    public void cancel() throws InterruptedException {
        thread.stop();
        cancelled = true;
    }

    private Credential auth() throws IOException {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET,
                Collections.singleton(YouTubeScopes.YOUTUBE_UPLOAD)
        ).setDataStoreFactory(dataStoreFactory).build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private File preUpload() throws InterruptedException, IOException {
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.ODS) {
            File tmpFile = new File(videoFile.getParentFile(), System.currentTimeMillis() + ".mp4");
            tmpFile.deleteOnExit();

            String args = String.format(FFMPEG_ODS, videoFile.getName(), tmpFile.getName());

            CommandLine commandLine = new CommandLine(settings.getExportCommand());
            commandLine.addArguments(args);
            System.out.println("Re-encoding for ODS with " + settings.getExportCommand() + args);
            Process process = new ProcessBuilder(commandLine.toStrings()).directory(videoFile.getParentFile()).start();

            final AtomicBoolean active = new AtomicBoolean(true);
            final InputStream in = process.getErrorStream();
            new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    while (active.get()) {
                        char c = (char) in.read();
                        if (c == '\r') {
                            String str = sb.toString();
                            System.out.println(str);
                            if (str.startsWith("frame=")) {
                                str = str.substring(6).trim();
                                str = str.substring(0, str.indexOf(' '));
                                double frame = Integer.parseInt(str);
                                progress = Suppliers.ofInstance(frame / videoFrames);
                            }
                            sb = new StringBuilder();
                        } else {
                            sb.append(c);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int result;
            try {
                result = process.waitFor();
            } catch (InterruptedException e) {
                process.destroy();
                throw e;
            } finally {
                active.set(false);
            }
            if (result != 0) {
                throw new IOException("FFmpeg returned: " + result);
            }

            MetadataInjector.injectODSMetadata(tmpFile);

            return tmpFile;
        }
        return videoFile;
    }

    private Video doUpload(YouTube youTube, File processedFile) throws IOException {
        Video video = new Video();
        VideoStatus videoStatus = new VideoStatus();
        videoStatus.setPrivacyStatus(videoVisibility.name().toLowerCase());
        video.setStatus(videoStatus);
        video.setSnippet(videoSnippet);

        Video result;
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(processedFile))) {
            InputStreamContent content = new InputStreamContent("video/*", inputStream);
            content.setLength(processedFile.length());
            YouTube.Videos.Insert videoInsert = youTube.videos().insert("snippet,statistics,status", video, content);
            final MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            progress = () -> {
                try {
                    return uploader.getProgress();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            result = videoInsert.execute();
        }

        return result;
    }

    private void doThumbUpload(YouTube youTube, Video video) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(thumbnailImage)) {
            InputStreamContent content = new InputStreamContent("image/" + thumbnailFormat, inputStream);
            content.setLength(inputStream.available());
            youTube.thumbnails().set(video.getId(), content).execute();
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError.ErrorInfo info = e.getDetails().getErrors().get(0);
            if ("Authorization".equals(info.getLocation()) && "forbidden".equals(info.getReason())) {
                e.printStackTrace();
                // TODO: When rewriting, show popup gui
            } else {
                throw e;
            }
        }
    }

    private void postUpload(File processedFile) {
        if (processedFile != videoFile) {
            FileUtils.deleteQuietly(processedFile);
        }
    }

    public double getProgress() {
        return progress.get();
    }

    public enum State {
        AUTH, PREPARE_VIDEO, UPLOAD, CLEANUP
    }
}
