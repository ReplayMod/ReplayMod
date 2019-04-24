package com.replaymod.online.gui;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.utils.Patterns;
import com.replaymod.core.utils.Utils;
import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.api.ApiException;
import com.replaymod.online.api.replay.FileUploader;
import com.replaymod.online.api.replay.holders.Category;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.element.advanced.GuiTextArea;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

//#if MC>=11300
//#else
//$$ import net.minecraft.client.settings.GameSettings;
//#endif

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class GuiUploadReplay extends GuiScreen {
    public final GuiScreen parent;

    public final GuiTextField name = new GuiTextField().setHeight(20).setMaxLength(30)
            .setI18nHint("replaymod.gui.upload.namehint");
    public final GuiLabel durationLabel = new GuiLabel();
    public final GuiCheckbox hideServerIP = new GuiCheckbox();
    public final GuiDropdownMenu<Category> category = new GuiDropdownMenu<Category>().setHeight(20)
            .setValues(Category.values()).setToString(v -> I18n.format("replaymod.category") + ": " + v.toNiceString());
    public final GuiTextField tags = new GuiTextField().setHeight(20).setMaxLength(30)
            .setI18nHint("replaymod.gui.upload.tagshint");

    public final GuiTextArea description = new GuiTextArea()
            .setI18nHint("replaymod.gui.upload.descriptionhint")
            .setMaxTextWidth(1000).setMaxTextHeight(100).setMaxCharCount(1000);

    public final GuiButton startButton = new GuiButton().setI18nLabel("replaymod.gui.upload.start");
    public final GuiButton backButton = new GuiButton().setI18nLabel("replaymod.gui.back");

    public final GuiPanel inputPanel = new GuiPanel()
            .setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, name, durationLabel, hideServerIP, category, tags);

    public final GuiImage thumbnail = new GuiImage();
    public final GuiLabel thumbnailWarning = new GuiLabel().setColor(Colors.RED);

    public final GuiPanel topPanel = new GuiPanel()
            .setLayout(new GridLayout().setColumns(3).setSpacingX(5))
            .setLayout(new CustomLayout<GuiPanel>() {
                @Override
                protected void layout(GuiPanel container, int width, int height) {
                    pos(inputPanel, 0, 0);
                    width(inputPanel, width / 2 - 4);

                    y(thumbnail, y(inputPanel) + height(inputPanel) + 10);
                    width(thumbnail, Math.min(width(inputPanel), (height - y(thumbnail)) * 16 / 9));
                    height(thumbnail, width(thumbnail) * 9 / 16);
                    x(thumbnail, (width / 2 - 4) / 2 - width(thumbnail) / 2);

                    pos(thumbnailWarning, x(thumbnail) + 5, y(thumbnail) + 5);
                    size(thumbnailWarning, width(thumbnail) - 10, height(thumbnail) - 10);

                    pos(description, width / 2 + 4, 0);
                    size(description, width - x(description), height);
                }
            })
            .addElements(null, inputPanel, description, thumbnail, thumbnailWarning);

    public final GuiPanel buttonPanel = new GuiPanel()
            .setLayout(new GridLayout().setColumns(2).setSpacingX(6))
            .addElements(null, startButton, backButton);

    {
        setTitle(new GuiLabel().setI18nText("replaymod.gui.upload.title"));
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(topPanel, 5, 25);
                pos(buttonPanel, 5, height - height(buttonPanel) - 5);

                size(topPanel, width - 10, y(buttonPanel) - y(topPanel) - 10);
                width(buttonPanel, width - 10);
            }
        });
        addElements(null, topPanel, buttonPanel);
    }

    private final FileUploader uploader;

    public GuiUploadReplay(GuiScreen parent, ReplayModOnline mod, File file) {
        this.parent = parent;
        this.uploader = new FileUploader(mod.getApiClient());

        ReplayMetaData metaData;
        Optional<BufferedImage> optThumbnail;

        // Read from replay file
        try (ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), file)) {
            metaData = replayFile.getMetaData();
            optThumbnail = replayFile.getThumb();
        } catch (IOException e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Read replay file " + file.getName()));
        }

        // Apply to gui
        name.setText(Utils.fileNameToReplayName(file.getName()));
        int secs = metaData.getDuration() / 1000;
        durationLabel.setI18nText("replaymod.gui.upload.duration", secs / 60, secs % 60);
        hideServerIP.setEnabled(!metaData.isSingleplayer());
        hideServerIP.setI18nLabel("replaymod.gui.upload.hideip2", metaData.getServerName());

        if (optThumbnail.isPresent()) {
            // Thumbnail is preset, just add the thumbnail to the panel
            thumbnail.setTexture(optThumbnail.get());
        } else {
            // Get name of key used for thumbnail creation
            KeyBindingRegistry registry = mod.getCore().getKeyBindingRegistry();
            KeyBinding keyBinding = registry.getKeyBindings().get("replaymod.input.thumbnail");
            //#if MC>=11300
            String keyName = keyBinding == null ? "???" : keyBinding.func_197978_k();
            //#else
            //$$ String keyName = keyBinding == null ? "???" : GameSettings.getKeyDisplayString(keyBinding.getKeyCode());
            //#endif

            // No thumbnail, show default thumbnail and hint on how to create one
            thumbnail.setTexture(Utils.DEFAULT_THUMBNAIL);
            thumbnailWarning.setI18nText("replaymod.gui.upload.nothumbnail", keyName);
        }

        backButton.onClick(parent::display);

        startButton.onClick(() -> {
            GuiProgressPopup popup = new GuiProgressPopup(this);
            popup.open();

            // Read values
            String name = GuiUploadReplay.this.name.getText();
            Category category = GuiUploadReplay.this.category.getSelectedValue();
            String desc = StringUtils.join(description.getText(), '\n');
            Set<String> tagSet = stream(tags.getText().split(",")).collect(Collectors.toSet());
            tagSet.remove(""); // Remove the empty tag (if it exists)

            // Start upload
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Check if we need to remove the server address
                        if (hideServerIP.isChecked()) {
                            File tmp = File.createTempFile("replay_hidden_ip", "mcpr");

                            // Overide the server name
                            try (ReplayFile replay = new ZipReplayFile(new ReplayStudio(), file)) {
                                ReplayMetaData newMetaData = new ReplayMetaData(metaData);
                                newMetaData.setServerName(null);
                                replay.writeMetaData(newMetaData);
                                replay.saveTo(tmp);
                            }

                            // Upload modified file
                            uploader.uploadFile(tmp, name, tagSet, category, desc,
                                    progress -> popup.progressBar.setProgress(progress.floatValue()));

                            FileUtils.deleteQuietly(tmp);
                        } else {
                            // Upload original file
                            uploader.uploadFile(file, name, tagSet, category, desc,
                                    progress -> popup.progressBar.setProgress(progress.floatValue()));
                        }

                        // Success
                        mod.getCore().runLater(popup::close);
                    } catch (FileUploader.CancelledException ignored) {
                        // Cancelled
                        mod.getCore().runLater(popup::close);
                    } catch (Exception e) {
                        // Failure
                        e.printStackTrace();

                        String message;
                        if (e instanceof ApiException) {
                            message = e.getLocalizedMessage();
                        } else {
                            message = I18n.format("replaymod.gui.unknownerror");
                        }
                        mod.getCore().runLater(() -> {
                            // Show error popup
                            GuiYesNoPopup errorPopup = GuiYesNoPopup.open(GuiUploadReplay.this,
                                    new GuiLabel().setText(message).setColor(Colors.BLACK),
                                    new GuiLabel().setI18nText("replaymod.gui.upload.tryagain").setColor(Colors.BLACK))
                                    .setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no");
                            Futures.addCallback(errorPopup.getFuture(), new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(@Nullable Boolean result) {
                                    popup.close();
                                    if (result == Boolean.TRUE) {
                                        startButton.onClick();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    t.printStackTrace();
                                    popup.close();
                                }
                            });
                        });
                    }
                }
            }, "replaymod-file-uploader").start();
        });

        validateInputs();
        name.onTextChanged(s -> validateInputs());
        tags.onTextChanged(s -> validateInputs());
    }

    public void validateInputs() {
        if (name.getText().trim().length() < 5 || name.getText().trim().length() > 30) {
            name.setTextColor(Colors.RED);
            startButton.setDisabled().setTooltip(new GuiTooltip().setI18nText("replaymod.gui.upload.error.name.length"));
        } else if (!Patterns.ALPHANUMERIC_SPACE_HYPHEN_UNDERSCORE.matcher(name.getText()).matches()) {
            name.setTextColor(Colors.RED);
            startButton.setDisabled().setTooltip(new GuiTooltip().setI18nText("replaymod.gui.upload.error.name"));
        } else if (!Patterns.ALPHANUMERIC_COMMA.matcher(tags.getText()).matches()) {
            tags.setTextColor(Colors.RED);
            startButton.setDisabled().setTooltip(new GuiTooltip().setI18nText("replaymod.gui.upload.error.tags"));
        } else {
            name.setTextColor(Colors.WHITE);
            tags.setTextColor(Colors.WHITE);
            startButton.setEnabled().setTooltip(null);
        }
    }

    @Override
    protected GuiUploadReplay getThis() {
        return this;
    }

    private final class GuiProgressPopup extends AbstractGuiPopup<GuiProgressPopup> {
        public final GuiProgressBar progressBar = new GuiProgressBar().setSize(400, 20);
        public final GuiButton cancelButton = new GuiButton().setSize(200, 20).setI18nLabel("replaymod.gui.cancel");

        {
            setBackgroundColor(Colors.DARK_TRANSPARENT);
            popup.setLayout(new VerticalLayout().setSpacing(5));
            popup.addElements(new VerticalLayout.Data(0.5), progressBar, cancelButton);

            cancelButton.onClick(uploader::cancelUploading);
        }

        public GuiProgressPopup(GuiContainer container) {
            super(container);
        }

        @Override
        public void open() {
            super.open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected GuiProgressPopup getThis() {
            return this;
        }
    }
}
