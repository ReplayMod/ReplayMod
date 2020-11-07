package com.replaymod.render.gui;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonSyntaxException;
import com.replaymod.core.ReplayMod;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiColorPicker;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiFileChooserPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.Utils;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.crash.CrashReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static com.replaymod.core.utils.Utils.error;
import static com.replaymod.render.ReplayModRender.LOGGER;

//#if MC>=11400
import net.minecraft.text.TranslatableText;
//#endif

public class GuiRenderSettings extends AbstractGuiPopup<GuiRenderSettings> {
    { disablePopupBackground(); }
    public final GuiPanel contentPanel = new GuiPanel(popup).setBackgroundColor(new Color(0, 0, 0, 230));
    public final GuiVerticalList settingsList = new GuiVerticalList(contentPanel).setDrawSlider(true);

    public final GuiDropdownMenu<RenderSettings.RenderMethod> renderMethodDropdown =
            new GuiDropdownMenu<RenderSettings.RenderMethod>().onSelection(new Consumer<Integer>() {
                @Override
                public void consume(Integer old) {
                    if (renderMethodDropdown.getSelectedValue() == RenderSettings.RenderMethod.BLEND
                            ^ encodingPresetDropdown.getSelectedValue() == RenderSettings.EncodingPreset.BLEND) {
                        if (renderMethodDropdown.getSelectedValue() == RenderSettings.RenderMethod.BLEND) {
                            encodingPresetDropdown.setSelected(RenderSettings.EncodingPreset.BLEND);
                        } else {
                            encodingPresetDropdown.setSelected(RenderSettings.EncodingPreset.MP4_DEFAULT);
                        }
                    }
                    updateInputs();
                }
            }).setMinSize(new Dimension(0, 20)).setValues(RenderSettings.RenderMethod.getSupported());

    {
        for (Map.Entry<RenderSettings.RenderMethod, IGuiClickable> entry :
                renderMethodDropdown.getDropdownEntries().entrySet()) {
            entry.getValue().setTooltip(new GuiTooltip().setText(entry.getKey().getDescription()));
        }
    }

    public final GuiDropdownMenu<RenderSettings.EncodingPreset> encodingPresetDropdown =
            new GuiDropdownMenu<RenderSettings.EncodingPreset>().onSelection(new Consumer<Integer>() {
                @Override
                public void consume(Integer old) {
                    RenderSettings.EncodingPreset newPreset = encodingPresetDropdown.getSelectedValue();
                    if (newPreset == RenderSettings.EncodingPreset.BLEND
                            && encodingPresetDropdown.getSelectedValue() == RenderSettings.EncodingPreset.BLEND) {
                        renderMethodDropdown.setSelected(RenderSettings.RenderMethod.BLEND);
                    }
                    // Update export arguments to match new Preset
                    exportArguments.setText(newPreset.getValue());
                    // Update output file ending
                    if (outputFile != null) {
                        outputFile = conformExtension(outputFile, newPreset);
                        outputFileButton.setLabel(outputFile.getName());
                    }
                    updateInputs();
                }
            }).setMinSize(new Dimension(0, 20)).setValues(RenderSettings.EncodingPreset.getSupported());

    public final GuiNumberField videoWidth = new GuiNumberField().setSize(50, 20).setMinValue(1).setValidateOnFocusChange(true);
    public final GuiNumberField videoHeight = new GuiNumberField().setSize(50, 20).setMinValue(1).setValidateOnFocusChange(true);
    public final GuiSlider frameRateSlider = new GuiSlider().onValueChanged(new Runnable() {
        @Override
        public void run() {
            frameRateSlider.setText(I18n.translate("replaymod.gui.rendersettings.framerate")
                    + ": " + (frameRateSlider.getValue() + 10));
        }
    }).setSize(122, 20).setSteps(110);
    public final GuiPanel videoResolutionPanel = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(2))
            .addElements(new HorizontalLayout.Data(0.5), videoWidth, new GuiLabel().setText("*"), videoHeight);

    public final GuiNumberField bitRateField = new GuiNumberField().setValue(10).setSize(50, 20).setValidateOnFocusChange(true);
    public final GuiDropdownMenu<String> bitRateUnit = new GuiDropdownMenu<String>()
            .setSize(50, 20).setValues("bps", "kbps", "mbps").setSelected("mbps");

    public final GuiButton outputFileButton = new GuiButton().setMinSize(new Dimension(0, 20)).onClick(new Runnable() {
        @Override
        public void run() {
            GuiFileChooserPopup popup = GuiFileChooserPopup.openSaveGui(GuiRenderSettings.this, "replaymod.gui.save",
                    encodingPresetDropdown.getSelectedValue().getFileExtension());
            popup.setFolder(outputFile.getParentFile());
            popup.setFileName(outputFile.getName());
            popup.onAccept(file -> {
                if (!file.getName().equals(outputFile.getName())) {
                    userDefinedOutputFileName = true;
                }
                outputFile = file;
                outputFileButton.setLabel(file.getName());
            });
        }
    });

    public final GuiPanel mainPanel = new GuiPanel()
            .addElements(new GridLayout.Data(1, 0.5),
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.renderer"), renderMethodDropdown,
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.presets"), encodingPresetDropdown,
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.customresolution"), videoResolutionPanel,
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.bitrate"), new GuiPanel().addElements(null,
                            new GuiPanel().addElements(null, bitRateField, bitRateUnit).setLayout(new HorizontalLayout()),
                            frameRateSlider).setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(3)),
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.outputfile"), outputFileButton)
            .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(5));

    public final GuiCheckbox nametagCheckbox = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.nametags");

    public final GuiPanel stabilizePanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(10));
    public final GuiCheckbox stabilizeYaw = new GuiCheckbox(stabilizePanel)
            .setI18nLabel("replaymod.gui.yaw");
    public final GuiCheckbox stabilizePitch = new GuiCheckbox(stabilizePanel)
            .setI18nLabel("replaymod.gui.pitch");
    public final GuiCheckbox stabilizeRoll = new GuiCheckbox(stabilizePanel)
            .setI18nLabel("replaymod.gui.roll");

    public final GuiCheckbox chromaKeyingCheckbox = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.chromakey");
    public final GuiColorPicker chromaKeyingColor = new GuiColorPicker().setSize(30, 15);

    public static final int MIN_SPHERICAL_FOV = 120;
    public static final int MAX_SPHERICAL_FOV = 360;
    public static final int SPHERICAL_FOV_STEP_SIZE = 5;
    public final GuiSlider sphericalFovSlider = new GuiSlider()
            .onValueChanged(new Runnable() {
                @Override
                public void run() {
                    sphericalFovSlider.setText(I18n.translate("replaymod.gui.rendersettings.sphericalFov")
                            + ": " + (MIN_SPHERICAL_FOV + sphericalFovSlider.getValue() * SPHERICAL_FOV_STEP_SIZE) + "°");

                    updateInputs();
                }
            }).setSize(200, 20).setSteps((MAX_SPHERICAL_FOV - MIN_SPHERICAL_FOV) / SPHERICAL_FOV_STEP_SIZE);

    public final GuiCheckbox injectSphericalMetadata = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.sphericalmetadata");

    public final GuiCheckbox depthMap = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.depthmap");

    public final GuiCheckbox cameraPathExport = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.camerapath");

    public final GuiDropdownMenu<RenderSettings.AntiAliasing> antiAliasingDropdown = new GuiDropdownMenu<RenderSettings.AntiAliasing>()
            .setSize(200, 20).setValues(RenderSettings.AntiAliasing.values()).setSelected(RenderSettings.AntiAliasing.NONE);

    public final GuiPanel advancedPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(15))
            .addElements(null, nametagCheckbox, new GuiPanel().setLayout(
                    new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(15))
                    .addElements(new GridLayout.Data(0, 0.5),
                            new GuiLabel().setI18nText("replaymod.gui.rendersettings.stabilizecamera"), stabilizePanel,
                            chromaKeyingCheckbox, chromaKeyingColor,
                            injectSphericalMetadata, sphericalFovSlider,
                            depthMap, new GuiLabel(),
                            cameraPathExport, new GuiLabel(),
                            new GuiLabel().setI18nText("replaymod.gui.rendersettings.antialiasing"), antiAliasingDropdown));

    public final GuiTextField exportCommand = new GuiTextField().setI18nHint("replaymod.gui.rendersettings.command")
            .setSize(55, 20).setMaxLength(100).onTextChanged((old) -> updateInputs());
    public final GuiTextField exportArguments = new GuiTextField().setI18nHint("replaymod.gui.rendersettings.arguments")
            .setMinSize(new Dimension(245, 20)).setMaxLength(500).onTextChanged((old) -> updateInputs());
    public final GuiButton exportReset = new GuiButton().setLabel("X")
            .setSize(20, 20)
            .onClick(() -> {
                exportCommand.setText("");
                exportArguments.setText(encodingPresetDropdown.getSelectedValue().getValue());
                updateInputs();
            });

    public final GuiPanel commandlinePanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10))
            .addElements(null,
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5)).addElements(null, exportCommand, exportArguments, exportReset),
                    new GuiLabel(new GuiPanel().setLayout(new CustomLayout<GuiPanel>() {
                        @Override
                        protected void layout(GuiPanel container, int width, int height) {
                            size(container.getChildren().iterator().next(), width, height);
                        }

                        @Override
                        public ReadableDimension calcMinSize(GuiContainer<?> container) {
                            return new Dimension(300, 50);
                        }
                    })).setI18nText("replaymod.gui.rendersettings.ffmpeg.description").getContainer());


    public final GuiPanel buttonPanel = new GuiPanel(contentPanel).setLayout(new HorizontalLayout().setSpacing(4));
    public final GuiButton queueButton = new GuiButton(buttonPanel)
            .setSize(100, 20)
            .setI18nLabel("replaymod.gui.rendersettings.addtoqueue");
    public final GuiButton renderButton = new GuiButton(buttonPanel).onClick(() -> ReplayMod.instance.runLaterWithoutLock(new Runnable() {
        @Override
        public void run() {
            // Closing this GUI ensures that settings are saved
            getMinecraft().openScreen(null);
            try {
                VideoRenderer videoRenderer = new VideoRenderer(save(false), replayHandler, timeline);
                videoRenderer.renderVideo();
            } catch (FFmpegWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                NoticeScreen errorScreen = new NoticeScreen(
                        //#if MC>=11400
                        getScreen()::display,
                        new TranslatableText("replaymod.gui.rendering.error.title"),
                        new TranslatableText("replaymod.gui.rendering.error.message")
                        //#else
                        //$$ I18n.format("replaymod.gui.rendering.error.title"),
                        //$$ I18n.format("replaymod.gui.rendering.error.message")
                        //#endif
                );
                getMinecraft().openScreen(errorScreen);
            } catch (FFmpegWriter.FFmpegStartupException e) {
                GuiExportFailed.tryToRecover(e, newSettings -> {
                    // Update settings with fixed ffmpeg arguments
                    exportArguments.setText(newSettings.getExportArguments());
                    // Restart rendering, this will also save the changed ffmpeg arguments
                    renderButton.onClick();
                });
            } catch (Throwable t) {
                error(LOGGER, GuiRenderSettings.this, CrashReport.create(t, "Rendering video"), () -> {});
                getScreen().display(); // Re-show the render settings gui and the new error popup
            }
        }
    })).setSize(100, 20).setI18nLabel("replaymod.gui.render");
    public final GuiButton cancelButton = new GuiButton(buttonPanel)
            .onClick(this::close)
            .setSize(100, 20)
            .setI18nLabel("replaymod.gui.cancel");

    {
        Utils.link(videoWidth, videoHeight, bitRateField);

        contentPanel.setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                size(settingsList, width, height - height(buttonPanel) - 25);
                pos(settingsList, width / 2 - width(settingsList) / 2, 5);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, y(settingsList) + height(settingsList) + 10);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension screenSize = getContainer().getMinSize();
                return new Dimension(screenSize.getWidth() - 40, screenSize.getHeight() - 40);
            }
        });

        settingsList.getListPanel().setLayout(new VerticalLayout().setSpacing(10))
                .addElements(new VerticalLayout.Data(0.5),
                        new GuiLabel().setI18nText("replaymod.gui.rendersettings.video"), mainPanel, new GuiPanel(),
                        new GuiLabel().setI18nText("replaymod.gui.rendersettings.advanced"), advancedPanel, new GuiPanel(),
                        new GuiLabel().setI18nText("replaymod.gui.rendersettings.commandline"), commandlinePanel);

        videoWidth.onTextChanged(new Consumer<String>() {
            @Override
            public void consume(String old) {
                updateInputs();
            }
        });
        videoHeight.onTextChanged(new Consumer<String>() {
            @Override
            public void consume(String obj) {
                updateInputs();
            }
        });
    }

    private final AbstractGuiScreen<?> screen;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private File outputFile;
    private boolean userDefinedOutputFileName;

    public GuiRenderSettings(AbstractGuiScreen<?> container, ReplayHandler replayHandler, Timeline timeline) {
        super(container);
        this.screen = container;
        this.replayHandler = replayHandler;
        this.timeline = timeline;

        String json = "{}";
        try {
            json = new String(Files.readAllBytes(getSettingsPath()), StandardCharsets.UTF_8);
        } catch (NoSuchFileException | FileNotFoundException ignored) {
        } catch (IOException e) {
            LOGGER.error("Reading render settings:", e);
        }
        RenderSettings settings = null;
        try {
            settings = new GsonBuilder()
                    .registerTypeAdapter(RenderSettings.class, (InstanceCreator<RenderSettings>) type -> getDefaultRenderSettings())
                    .registerTypeAdapter(ReadableColor.class, new Gson().getAdapter(Color.class))
                    .create().fromJson(json, RenderSettings.class);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Parsing render settings:", e);
            LOGGER.error("Raw JSON: {}", json);
        }
        if (settings == null) {
            settings = getDefaultRenderSettings();
        }
        load(settings);
    }

    protected void updateInputs() {
        RenderSettings.RenderMethod renderMethod = renderMethodDropdown.getSelectedValue();

        // Enable/Disable video with input
        videoWidth.setEnabled(!renderMethod.hasFixedAspectRatio());

        // Validate video width and height
        String resolutionError = updateResolution();
        if (resolutionError == null) {
            queueButton.setEnabled().setTooltip(null);
            videoWidth.setTextColor(Colors.WHITE);
            videoHeight.setTextColor(Colors.WHITE);
        } else {
            queueButton.setDisabled().setTooltip(new GuiTooltip().setI18nText(resolutionError));
            videoWidth.setTextColor(Colors.RED);
            videoHeight.setTextColor(Colors.RED);
        }

        String[] compatError = VideoRenderer.checkCompat();
        if (resolutionError != null) {
            renderButton.setDisabled().setTooltip(new GuiTooltip().setI18nText(resolutionError));
        } else if (compatError != null) {
            renderButton.setDisabled().setTooltip(new GuiTooltip().setText(compatError));
        } else {
            renderButton.setEnabled().setTooltip(null);
        }

        // Enable/Disable bitrate input field and dropdown
        if (encodingPresetDropdown.getSelectedValue().hasBitrateSetting()) {
            bitRateField.setEnabled();
            bitRateUnit.setEnabled();
        } else {
            bitRateField.setDisabled();
            bitRateUnit.setDisabled();
        }

        // Enable/Disable camera stabilization checkboxes
        switch (renderMethod) {
            case CUBIC:
            case EQUIRECTANGULAR:
            case ODS:
                stabilizePanel.invokeAll(IGuiCheckbox.class, GuiElement::setEnabled);
                break;
            default:
                stabilizePanel.invokeAll(IGuiCheckbox.class, GuiElement::setDisabled);
        }

        // Enable/Disable Spherical FOV slider
        sphericalFovSlider.setEnabled(renderMethod.isSpherical());

        // Enable/Disable inject metadata checkbox
        if (encodingPresetDropdown.getSelectedValue().getFileExtension().equals("mp4")
                && renderMethod.isSpherical()) {
            injectSphericalMetadata.setEnabled().setTooltip(null);
        } else {
            injectSphericalMetadata.setDisabled().setTooltip(new GuiTooltip().setColor(Colors.RED)
                    .setI18nText("replaymod.gui.rendersettings.sphericalmetadata.error"));
        }

        // Enable/Disable various options for blend export
        boolean isEXR = encodingPresetDropdown.getSelectedValue() == RenderSettings.EncodingPreset.EXR;
        boolean isBlend = renderMethod == RenderSettings.RenderMethod.BLEND;
        boolean isFFmpeg = !isBlend && !isEXR;
        if (isBlend) {
            videoWidth.setDisabled();
            videoHeight.setDisabled();
        }
        encodingPresetDropdown.setEnabled(!isBlend);
        exportCommand.setEnabled(isFFmpeg);
        exportArguments.setEnabled(isFFmpeg);
        antiAliasingDropdown.setEnabled(isFFmpeg);

        if (isEXR) {
            depthMap.setEnabled().setTooltip(null);
        } else {
            depthMap.setDisabled().setTooltip(new GuiTooltip().setColor(Colors.RED)
                    .setI18nText("replaymod.gui.rendersettings.depthmap.onlyexr"));
        }

        // Enable/Disable export args reset button
        boolean commandChanged = !exportCommand.getText().isEmpty();
        boolean argsChanged = !encodingPresetDropdown.getSelectedValue().getValue().equals(exportArguments.getText());
        exportReset.setEnabled(commandChanged || argsChanged);
    }

    protected String updateResolution() {
        RenderSettings.EncodingPreset preset = encodingPresetDropdown.getSelectedValue();
        RenderSettings.RenderMethod method = renderMethodDropdown.getSelectedValue();

        int videoHeight = this.videoHeight.getInteger();
        int videoWidth;
        if (method.hasFixedAspectRatio()) {
            // cubic rendering requires an aspect ratio of 4:3,
            // therefore the height must be divisible by 3
            if (method == RenderSettings.RenderMethod.CUBIC && videoHeight % 3 != 0) {
                return "replaymod.gui.rendersettings.customresolution.warning.cubic.height";
            }

            int sphericalFov = MIN_SPHERICAL_FOV + sphericalFovSlider.getValue() * SPHERICAL_FOV_STEP_SIZE;
            videoWidth = videoWidthForHeight(method, videoHeight, sphericalFov, sphericalFov);

            this.videoWidth.setValue(videoWidth);
        } else {
            videoWidth = this.videoWidth.getInteger();
        }

        // Make sure the export arguments haven't been changed manually
        if (exportArguments.getText().equals(preset.getValue())) {
            // Yuv420 requires both dimensions to be even
            if (preset.isYuv420()
                    && (videoWidth % 2 != 0 || videoHeight % 2 != 0)) {

                if (method == RenderSettings.RenderMethod.CUBIC) {
                    // cubic yuv rendering has the special case that the height must be
                    // divisible by both 3 and 2 - tell the user about it with a special message
                    return "replaymod.gui.rendersettings.customresolution.warning.yuv420.cubic";
                }

                return "replaymod.gui.rendersettings.customresolution.warning.yuv420";
            }
        }

        return null;
    }

    protected int videoWidthForHeight(RenderSettings.RenderMethod method, int height,
                                      int sphericalFovX, int sphericalFovY) {
        if (method.isSpherical()) {
            if (sphericalFovY < 180) {
                // calculate the non-cropped height of the video
                height = Math.round(height * 180 / (float) sphericalFovY);
            }

            int width = height * 2;

            if (sphericalFovX < 360) {
                // crop the resulting width
                width = Math.round(width * (float) sphericalFovX / 360);
            }

            if (method == RenderSettings.RenderMethod.ODS) {
                width = Math.round(width / 2f);
            }

            return width;

        } else if (method == RenderSettings.RenderMethod.CUBIC) {
            Preconditions.checkArgument(height % 3 == 0);
            return height / 3 * 4;
        }

        throw new IllegalArgumentException();
    }

    public void load(RenderSettings settings) {
        if (settings.getRenderMethod().isSupported()) {
            renderMethodDropdown.setSelected(settings.getRenderMethod());
        }
        RenderSettings.EncodingPreset encodingPreset = settings.getEncodingPreset();
        /* encodingPreset can be null from a previously supported and later removed preset */
        if (encodingPreset == null || !encodingPreset.isSupported()) {
            encodingPreset = getDefaultRenderSettings().getEncodingPreset();
        }
        encodingPresetDropdown.setSelected(encodingPreset);
        videoWidth.setValue(settings.getTargetVideoWidth());
        videoHeight.setValue(settings.getTargetVideoHeight());
        frameRateSlider.setValue(settings.getFramesPerSecond() - 10);
        if (settings.getBitRate() % (1 << 20) == 0) {
            bitRateField.setValue(settings.getBitRate() >> 20);
            bitRateUnit.setSelected(2);
        } else if (settings.getBitRate() % (1 << 10) == 0) {
            bitRateField.setValue(settings.getBitRate() >> 10);
            bitRateUnit.setSelected(1);
        } else {
            bitRateField.setValue(settings.getBitRate());
            bitRateUnit.setSelected(0);
        }
        File savedOutputFile = settings.getOutputFile();
        if (savedOutputFile == null || !savedOutputFile.getParentFile().exists()) {
            this.outputFile = generateOutputFile(encodingPreset);
            userDefinedOutputFileName = false;
        } else if (savedOutputFile.exists()) {
            String name = generateOutputFile(encodingPreset).getName();
            boolean isFolder = savedOutputFile.isDirectory() && !savedOutputFile.getName().endsWith(".exr");
            this.outputFile = new File(isFolder ? savedOutputFile : savedOutputFile.getParentFile(), name);
            userDefinedOutputFileName = false;
        } else {
            this.outputFile = conformExtension(savedOutputFile, encodingPreset);
            userDefinedOutputFileName = true;
        }
        outputFileButton.setLabel(this.outputFile.getName());
        nametagCheckbox.setChecked(settings.isRenderNameTags());
        stabilizeYaw.setChecked(settings.isStabilizeYaw());
        stabilizePitch.setChecked(settings.isStabilizePitch());
        stabilizeRoll.setChecked(settings.isStabilizeRoll());
        if (settings.getChromaKeyingColor() == null) {
            chromaKeyingCheckbox.setChecked(false);
            chromaKeyingColor.setColor(Colors.GREEN);
        } else {
            chromaKeyingCheckbox.setChecked(true);
            chromaKeyingColor.setColor(settings.getChromaKeyingColor());
        }
        sphericalFovSlider.setValue((settings.getSphericalFovX() - MIN_SPHERICAL_FOV) / SPHERICAL_FOV_STEP_SIZE);
        injectSphericalMetadata.setChecked(settings.isInjectSphericalMetadata());
        depthMap.setChecked(settings.isDepthMap());
        cameraPathExport.setChecked(settings.isCameraPathExport());
        antiAliasingDropdown.setSelected(settings.getAntiAliasing());
        exportCommand.setText(settings.getExportCommand());
        String exportArguments = settings.getExportArguments();
        if (exportArguments == null || settings.getEncodingPreset() == null) {
            // backwards compat, see RenderSettings#exportArguments
            exportArguments = encodingPreset.getValue();
        }
        this.exportArguments.setText(exportArguments);

        updateInputs();
    }

    public RenderSettings save(boolean serialize) {
        int sphericalFov = MIN_SPHERICAL_FOV + sphericalFovSlider.getValue() * SPHERICAL_FOV_STEP_SIZE;

        return new RenderSettings(
                renderMethodDropdown.getSelectedValue(),
                encodingPresetDropdown.getSelectedValue(),
                videoWidth.getInteger(),
                videoHeight.getInteger(),
                frameRateSlider.getValue() + 10,
                bitRateField.getInteger() << (10 * bitRateUnit.getSelected()),
                serialize && !userDefinedOutputFileName ? outputFile.getParentFile() : outputFile,
                nametagCheckbox.isChecked(),
                stabilizeYaw.isChecked() && (serialize || stabilizeYaw.isEnabled()),
                stabilizePitch.isChecked() && (serialize || stabilizePitch.isEnabled()),
                stabilizeRoll.isChecked() && (serialize || stabilizeRoll.isEnabled()),
                chromaKeyingCheckbox.isChecked() ? chromaKeyingColor.getColor() : null,
                sphericalFov, Math.min(180, sphericalFov),
                injectSphericalMetadata.isChecked() && (serialize || injectSphericalMetadata.isEnabled()),
                depthMap.isChecked() && (serialize || depthMap.isEnabled()),
                cameraPathExport.isChecked(),
                serialize || antiAliasingDropdown.isEnabled() ? antiAliasingDropdown.getSelectedValue() : RenderSettings.AntiAliasing.NONE,
                exportCommand.getText(),
                exportArguments.getText(),
                net.minecraft.client.gui.screen.Screen.hasControlDown()
        );
    }

    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        File folder = ReplayModRender.instance.getVideoFolder();
        return new File(folder, fileName + "." + encodingPreset.getFileExtension());
    }

    public AbstractGuiScreen<?> getScreen() {
        return screen;
    }

    public void setOutputFileBaseName(String base) {
        RenderSettings.EncodingPreset preset = encodingPresetDropdown.getSelectedValue();
        File file = new File(outputFile.getParentFile(), base + "." + preset.getFileExtension());
        // Ensure the file name is valid
        try {
            //noinspection ResultOfMethodCallIgnored
            file.toPath();
            outputFile = file;
            outputFileButton.setLabel(file.getName());
        } catch (InvalidPathException ignored) {
            setOutputFileBaseName("filename_invalid_" + base.hashCode());
        }
    }

    protected File conformExtension(File file, RenderSettings.EncodingPreset preset) {
        String name = file.getName();
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return new File(file.getParentFile(), name + "." + preset.getFileExtension());
    }

    protected Path getSettingsPath() {
        return ReplayModRender.instance.getRenderSettingsPath();
    }

    private RenderSettings getDefaultRenderSettings() {
        return new RenderSettings(RenderSettings.RenderMethod.DEFAULT, RenderSettings.EncodingPreset.MP4_DEFAULT, 1920, 1080, 60, 10 << 20, null,
                true, false, false, false, null, 360, 180, false, false, false, RenderSettings.AntiAliasing.NONE, "", RenderSettings.EncodingPreset.MP4_DEFAULT.getValue(), false);
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    public void close() {
        RenderSettings settings = save(true);
        String json = new Gson().toJson(settings);
        try {
            Files.write(getSettingsPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Saving render settings:", e);
        }
        super.close();
    }

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    @Override
    protected GuiRenderSettings getThis() {
        return this;
    }

    public static GuiScreen createBaseScreen() {
        GuiScreen screen = new GuiScreen();
        screen.setBackground(AbstractGuiScreen.Background.NONE);
        return screen;
    }
}
