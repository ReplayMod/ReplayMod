package com.replaymod.extras.youtube;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.element.advanced.GuiTextArea;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiFileChooserPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.versions.Image;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.resource.language.I18n;
import org.apache.commons.io.IOUtils;

//#if MC>=11300
import net.minecraft.util.SystemUtil;
//#else
//$$ import org.lwjgl.Sys;
//#endif

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

import static com.replaymod.extras.ReplayModExtras.LOGGER;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class GuiYoutubeUpload extends GuiScreen {
    private final GuiScreen previousScreen;
    private final File videoFile;
    private final int videoFrames;
    private final RenderSettings settings;
    private String thumbnailFormat;
    private byte[] thumbnailImage;

    public final Runnable inputValidation = new Runnable() {
        @Override
        public void run() {
            String problem = null;

            if (nameField.getText().isEmpty()) problem = "replaymod.gui.titleempty";
            if (thumbnailImage != null) {
                if (thumbnailImage.length > 1024 * 1024 * 2) problem = "replaymod.gui.videothumbnailtoolarge";
                if (!asList("jpeg", "png").contains(thumbnailFormat)) problem = "replaymod.gui.videothumbnailformat";
            }

            if (upload == null) {
                if (problem == null) {
                    uploadButton.setEnabled();
                    uploadButton.setTooltip(null);
                } else {
                    uploadButton.setDisabled();
                    uploadButton.setTooltip(new GuiTooltip().setI18nText(problem));
                }
            }
        }
    };

    public final GuiTextField nameField = new GuiTextField().setI18nHint("replaymod.gui.videotitle")
            .onTextChanged(s -> inputValidation.run());

    public final GuiTextArea descriptionField = new GuiTextArea().setMaxCharCount(Integer.MAX_VALUE)
            .setMaxTextWidth(Integer.MAX_VALUE).setMaxTextHeight(Integer.MAX_VALUE);
    {
        descriptionField.setText(new String[]{I18n.translate("replaymod.gui.videodescription")});
    }

    public final GuiTextField tagsField = new GuiTextField().setI18nHint("replaymod.gui.videotags");

    {
        nameField.setNext(descriptionField)
                .getNext().setNext(tagsField)
                .getNext().setNext(nameField);
    }

    public final GuiProgressBar progressBar = new GuiProgressBar();

    public final GuiPanel leftPanel = new GuiPanel(this).setLayout(new CustomLayout<GuiPanel>() {
        @Override
        protected void layout(GuiPanel container, int width, int height) {
            size(nameField, width, 20);
            size(descriptionField, width, height - 90);
            size(tagsField, width, 20);
            size(progressBar, width, 20);

            pos(nameField, 0, 0);
            pos(descriptionField, 0, 30);
            pos(tagsField, 0, height - 50);
            pos(progressBar, 0, height - 20);
        }
    }).addElements(null, nameField, descriptionField, tagsField, progressBar);

    public final GuiDropdownMenu<VideoVisibility> visibilityDropdown = new GuiDropdownMenu<VideoVisibility>()
            .setSize(200, 20).setValues(VideoVisibility.values()).setSelected(VideoVisibility.PUBLIC);

    public final GuiButton thumbnailButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            Futures.addCallback(
                    GuiFileChooserPopup.openLoadGui(GuiYoutubeUpload.this, "replaymod.gui.load",
                            ImageIO.getReaderFileSuffixes()).getFuture(),
                    new FutureCallback<File>() {
                        @Override
                        public void onSuccess(@Nullable File result) {
                            if (result != null) {
                                thumbnailButton.setLabel(result.getName());
                                Image image;
                                try {
                                    thumbnailImage = IOUtils.toByteArray(new FileInputStream(result));
                                    ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(thumbnailImage));
                                    ImageReader reader = ImageIO.getImageReaders(in).next();
                                    thumbnailFormat = reader.getFormatName().toLowerCase();
                                    image = Image.read(new ByteArrayInputStream(thumbnailImage));
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    thumbnailImage = null;
                                    image = null;
                                }
                                thumbnail.setTexture(image);
                                inputValidation.run();
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            throw new RuntimeException(t);
                        }
                    });
        }
    }).setSize(200, 20).setI18nLabel("replaymod.gui.videothumbnail");

    public final GuiImage thumbnail = new GuiImage().setSize(200, 112).setTexture(Utils.DEFAULT_THUMBNAIL);

    public final GuiButton uploadButton = new GuiButton(this).setSize(98, 20);

    public final GuiButton closeButton = new GuiButton(this).onClick(new Runnable() {
        @Override
        public void run() {
            previousScreen.display();
        }
    }).setSize(98, 20).setI18nLabel("replaymod.gui.back");

    public final GuiPanel rightPanel = new GuiPanel(this)
            .addElements(null, visibilityDropdown, thumbnailButton, thumbnail)
            .setLayout(new VerticalLayout(VerticalLayout.Alignment.TOP).setSpacing(10));

    private YoutubeUploader upload;

    {
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(leftPanel, 10, 10);
                size(leftPanel, width - 200 - 30, height - 20);

                pos(rightPanel, width - 210, 10);
                pos(uploadButton, width - 210, height - 30);
                pos(closeButton, width - 108, height - 30);
            }
        });

        setState(false);
        inputValidation.run();
    }

    private void setState(boolean uploading) {
        forEach(GuiElement.class).setEnabled(!uploading);
        uploadButton.setEnabled();
        if (uploading) {
            uploadButton.onClick(() -> {
                setState(false);
                new Thread(() -> {
                    try {
                        upload.cancel();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }).setI18nLabel("replaymod.gui.cancel");
        } else {
            uploadButton.onClick(() -> {
                try {
                    setState(true);

                    VideoVisibility visibility = visibilityDropdown.getSelectedValue();
                    VideoSnippet snippet = new VideoSnippet();
                    snippet.setTitle(nameField.getText());
                    snippet.setDescription(Strings.join(descriptionField.getText(), "\n"));
                    snippet.setTags(asList(tagsField.getText().split(",")));
                    upload = new YoutubeUploader(getMinecraft(), videoFile, videoFrames,
                            thumbnailFormat, thumbnailImage, settings, visibility, snippet);
                    ListenableFuture<Video> future = upload.upload();
                    Futures.addCallback(future, new FutureCallback<Video>() {
                        @Override
                        public void onSuccess(Video result) {
                            String url = "https://youtu.be/" + result.getId();
                            try {
                                MCVer.openURL(new URL(url).toURI());
                            } catch(Throwable throwable) {
                                LOGGER.error("Failed to open video URL \"{}\":", url, throwable);
                            }
                            upload = null;
                            progressBar.setLabel(I18n.translate("replaymod.gui.ytuploadprogress.done", url));
                            setState(false);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            if (t instanceof InterruptedException || upload.isCancelled()) {
                                progressBar.setProgress(0);
                                progressBar.setLabel("%d%%");
                            } else {
                                t.printStackTrace();
                                progressBar.setLabel(t.getLocalizedMessage());
                            }
                            upload = null;
                            setState(false);
                        }
                    });
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }).setI18nLabel("replaymod.gui.upload");
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (upload != null && upload.getState() != null) {
            progressBar.setProgress((float) upload.getProgress());
            progressBar.setI18nLabel("replaymod.gui.ytuploadprogress." + upload.getState().name().toLowerCase());
        }
        super.draw(renderer, size, renderInfo);
    }
}
