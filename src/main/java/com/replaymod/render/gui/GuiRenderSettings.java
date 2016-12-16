package com.replaymod.render.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiColorPicker;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiFileChooserPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.Utils;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class GuiRenderSettings extends GuiScreen implements Closeable {
    public final GuiPanel contentPanel = new GuiPanel(this).setBackgroundColor(Colors.DARK_TRANSPARENT);
    public final GuiVerticalList settingsList = new GuiVerticalList(contentPanel).setDrawSlider(true);

    public final GuiDropdownMenu<RenderSettings.RenderMethod> renderMethodDropdown =
            new GuiDropdownMenu<RenderSettings.RenderMethod>().onSelection(new Consumer<Integer>() {
                @Override
                public void consume(Integer old) {
                    updateInputs();
                }
            }).setMinSize(new Dimension(0, 20)).setValues(RenderSettings.RenderMethod.values());

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
                    // Update export arguments to match new Preset
                    exportArguments.setText(newPreset.getValue());
                    // If the user hasn't changed the output file by themselves,
                    if (!outputFileManuallySet) {
                        // generate a new output file name with updated file extension
                        outputFile = generateOutputFile(newPreset);
                        outputFileButton.setLabel(outputFile.getName());
                    }
                    updateInputs();
                }
            }).setMinSize(new Dimension(0, 20)).setValues(RenderSettings.EncodingPreset.values());

    public final GuiNumberField videoWidth = new GuiNumberField().setSize(50, 20).setMinValue(1);
    public final GuiNumberField videoHeight = new GuiNumberField().setSize(50, 20).setMinValue(1);
    public final GuiSlider frameRateSlider = new GuiSlider().onValueChanged(new Runnable() {
        @Override
        public void run() {
            frameRateSlider.setText(I18n.format("replaymod.gui.rendersettings.framerate")
                    + ": " + (frameRateSlider.getValue() + 10));
        }
    }).setSize(122, 20).setSteps(110);
    public final GuiPanel videoResolutionPanel = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(2))
            .addElements(new HorizontalLayout.Data(0.5), videoWidth, new GuiLabel().setText("*"), videoHeight);

    public final GuiNumberField bitRateField = new GuiNumberField().setValue(10).setSize(50, 20);
    public final GuiDropdownMenu<String> bitRateUnit = new GuiDropdownMenu<String>()
            .setSize(50, 20).setValues("bps", "kbps", "mbps").setSelected("mbps");

    public final GuiButton outputFileButton = new GuiButton().setMinSize(new Dimension(0, 20)).onClick(new Runnable() {
        @Override
        public void run() {
            Futures.addCallback(
                    GuiFileChooserPopup.openSaveGui(GuiRenderSettings.this, "replaymod.gui.save",
                            encodingPresetDropdown.getSelectedValue().getFileExtension()).getFuture(),
                    new FutureCallback<File>() {
                        @Override
                        public void onSuccess(@Nullable File result) {
                            if (result != null) {
                                outputFile = result;
                                outputFileManuallySet = true;
                                outputFileButton.setLabel(result.getName());
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            throw new RuntimeException(t);
                        }
                    });
        }
    });

    public final GuiPanel mainPanel = new GuiPanel()
            .addElements(new GridLayout.Data(1, 0.5),
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.renderer"), renderMethodDropdown,
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.presets"), encodingPresetDropdown,
                    new GuiLabel().setI18nText("replaymod.gui.rendersettings.customresolution"), videoResolutionPanel,
                    new GuiLabel().setI18nText("replaymod.gui.settings.bitrate"), new GuiPanel().addElements(null,
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

    public final GuiCheckbox inject360Metadata = new GuiCheckbox()
            .setI18nLabel("replaymod.gui.rendersettings.360metadata");

    public final GuiDropdownMenu<RenderSettings.AntiAliasing> antiAliasingDropdown = new GuiDropdownMenu<RenderSettings.AntiAliasing>()
            .setSize(200, 20).setValues(RenderSettings.AntiAliasing.values()).setSelected(RenderSettings.AntiAliasing.NONE);

    public final GuiPanel advancedPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(15))
            .addElements(null, nametagCheckbox, new GuiPanel().setLayout(
                    new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(15))
                    .addElements(new GridLayout.Data(0, 0.5),
                            new GuiLabel().setI18nText("replaymod.gui.rendersettings.stabilizecamera"), stabilizePanel,
                            chromaKeyingCheckbox, chromaKeyingColor,
                            inject360Metadata,
                            new GuiLabel(), // to show the anti-aliasing options in a new line
                            new GuiLabel().setI18nText("replaymod.gui.rendersettings.antialiasing"), antiAliasingDropdown));

    public final GuiTextField exportCommand = new GuiTextField().setI18nHint("replaymod.gui.rendersettings.command")
            .setSize(55, 20).setMaxLength(100);
    public final GuiTextField exportArguments = new GuiTextField().setI18nHint("replaymod.gui.rendersettings.arguments")
            .setMinSize(new Dimension(245, 20)).setMaxLength(500);

    public final GuiPanel commandlinePanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10))
            .addElements(null,
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5)).addElements(null, exportCommand, exportArguments),
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
    public final GuiButton renderButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            // Closing this GUI ensures that settings are saved
            getMinecraft().displayGuiScreen(null);
            try {
                VideoRenderer videoRenderer = new VideoRenderer(save(false), replayHandler, timeline);
                videoRenderer.renderVideo();
            } catch (IOException e) {
                e.printStackTrace();

                GuiErrorScreen errorScreen = new GuiErrorScreen(I18n.format("replaymod.gui.rendering.error.title"),
                        I18n.format("replaymod.gui.rendering.error.message"));
                getMinecraft().displayGuiScreen(errorScreen);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Rendering video");
                throw new ReportedException(crashReport);
            }
        }
    }).setSize(148, 20).setI18nLabel("replaymod.gui.render");
    public final GuiButton cancelButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            getMinecraft().displayGuiScreen(null);
        }
    }).setSize(148, 20).setI18nLabel("replaymod.gui.cancel");

    {
        setBackground(Background.NONE);
        Utils.link(videoWidth, videoHeight, bitRateField);
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(contentPanel, width / 2 - width(contentPanel) / 2, height / 2 - height(contentPanel) / 2);
            }
        });

        contentPanel.setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                size(settingsList, width, height - height(buttonPanel) - 25);
                pos(settingsList, width / 2 - width(settingsList) / 2, 5);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, y(settingsList) + height(settingsList) + 10);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension screenSize = getMinSize();
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

    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private File outputFile;
    private boolean outputFileManuallySet;

    public GuiRenderSettings(ReplayHandler replayHandler, Timeline timeline) {
        this.replayHandler = replayHandler;
        this.timeline = timeline;

        String json = getConfigProperty(ReplayModRender.instance.getConfiguration()).getString();
        RenderSettings settings = new GsonBuilder().registerTypeAdapter(RenderSettings.class, new InstanceCreator<RenderSettings>() {
                    @Override
                    public RenderSettings createInstance(Type type) {
                        return getDefaultRenderSettings();
                    }
                }).create().fromJson(json, RenderSettings.class);
        load(settings);
    }

    protected void updateInputs() {
        // Validate video width and height
        String error = isResolutionValid();
        if (error == null) {
            renderButton.setEnabled().setTooltip(null);
            videoWidth.setTextColor(Colors.WHITE);
            videoHeight.setTextColor(Colors.WHITE);
        } else {
            renderButton.setDisabled().setTooltip(new GuiTooltip().setI18nText(error));
            videoWidth.setTextColor(Colors.RED);
            videoHeight.setTextColor(Colors.RED);
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
        switch (renderMethodDropdown.getSelectedValue()) {
            case CUBIC:
            case EQUIRECTANGULAR:
            case ODS:
                stabilizePanel.forEach(IGuiCheckbox.class).setEnabled();
                break;
            default:
                stabilizePanel.forEach(IGuiCheckbox.class).setDisabled();
        }

        // Enable/Disable inject metadata checkbox
        if (encodingPresetDropdown.getSelectedValue().getFileExtension().equals("mp4")
                && (renderMethodDropdown.getSelectedValue() == RenderSettings.RenderMethod.EQUIRECTANGULAR
                || renderMethodDropdown.getSelectedValue() == RenderSettings.RenderMethod.ODS)) {
            inject360Metadata.setEnabled().setTooltip(null);
        } else {
            inject360Metadata.setDisabled().setTooltip(new GuiTooltip().setColor(Colors.RED)
                    .setI18nText("replaymod.gui.rendersettings.360metadata.error"));
        }
    }

    protected String isResolutionValid() {
        RenderSettings.EncodingPreset preset = encodingPresetDropdown.getSelectedValue();
        RenderSettings.RenderMethod method = renderMethodDropdown.getSelectedValue();
        int videoWidth = this.videoWidth.getInteger();
        int videoHeight = this.videoHeight.getInteger();

        // Make sure the export arguments haven't been changed manually
        if (exportArguments.getText().equals(preset.getValue())) {
            // Yuv420 requires both dimensions to be even
            if (preset.isYuv420()
                    && (videoWidth % 2 != 0 || videoHeight % 2 != 0)) {
                return "replaymod.gui.rendersettings.customresolution.warning.yuv420";
            }
        }

        if (method == RenderSettings.RenderMethod.CUBIC
                && (videoWidth * 3 / 4 != videoHeight || videoWidth * 3 % 4 != 0)) {
            return "replaymod.gui.rendersettings.customresolution.warning.cubic";
        }
        if (method == RenderSettings.RenderMethod.EQUIRECTANGULAR
                && (videoWidth / 2 != videoHeight || videoWidth % 2 != 0)) {
            return "replaymod.gui.rendersettings.customresolution.warning.equirectangular";
        }
        if (method == RenderSettings.RenderMethod.ODS
                && videoWidth != videoHeight) {
            return "replaymod.gui.rendersettings.customresolution.warning.ods";
        }
        return null;
    }

    public void load(RenderSettings settings) {
        renderMethodDropdown.setSelected(settings.getRenderMethod());
        encodingPresetDropdown.setSelected(settings.getEncodingPreset());
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
        if (settings.getOutputFile() == null) {
            outputFile = generateOutputFile(settings.getEncodingPreset());
            outputFileManuallySet = false;
        } else {
            outputFile = settings.getOutputFile();
            outputFileManuallySet = true;
        }
        outputFileButton.setLabel(outputFile.getName());
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
        inject360Metadata.setChecked(settings.isInject360Metadata());
        antiAliasingDropdown.setSelected(settings.getAntiAliasing());
        exportCommand.setText(settings.getExportCommand());
        exportArguments.setText(settings.getExportArguments());

        updateInputs();
    }

    public RenderSettings save(boolean serialize) {
        return new RenderSettings(
                renderMethodDropdown.getSelectedValue(),
                encodingPresetDropdown.getSelectedValue(),
                videoWidth.getInteger(),
                videoHeight.getInteger(),
                frameRateSlider.getValue() + 10,
                bitRateField.getInteger() << (10 * bitRateUnit.getSelected()),
                serialize ? null : outputFile,
                nametagCheckbox.isChecked(),
                stabilizeYaw.isChecked() && (serialize || stabilizeYaw.isEnabled()),
                stabilizePitch.isChecked() && (serialize || stabilizePitch.isEnabled()),
                stabilizeRoll.isChecked() && (serialize || stabilizeRoll.isEnabled()),
                chromaKeyingCheckbox.isChecked() ? chromaKeyingColor.getColor() : null,
                inject360Metadata.isChecked() && (serialize || inject360Metadata.isEnabled()),
                antiAliasingDropdown.getSelectedValue(),
                exportCommand.getText(),
                exportArguments.getText(),
                net.minecraft.client.gui.GuiScreen.isCtrlKeyDown()
        );
    }

    private File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        File folder = ReplayModRender.instance.getVideoFolder();
        return new File(folder, fileName + "." + encodingPreset.getFileExtension());
    }

    private RenderSettings getDefaultRenderSettings() {
        return new RenderSettings(RenderSettings.RenderMethod.DEFAULT, RenderSettings.EncodingPreset.MP4_DEFAULT, 1920, 1080, 60, 10 << 20, null,
                true, false, false, false, null, false, RenderSettings.AntiAliasing.NONE, "", RenderSettings.EncodingPreset.MP4_DEFAULT.getValue(), false);
    }

    @Override
    public void close() {
        RenderSettings settings = save(true);
        String json = new Gson().toJson(settings);
        Configuration config = ReplayModRender.instance.getConfiguration();
        getConfigProperty(config).set(json);
        config.save();
    }

    private Property getConfigProperty(Configuration configuration) {
        return configuration.get("rendersettings", "settings", "{}",
                "Last state of the render settings GUI. Internal use only.");
    }
}
