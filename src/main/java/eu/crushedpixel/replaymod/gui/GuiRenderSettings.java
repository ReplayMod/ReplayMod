package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.CheckBoxListener;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.EncodingPreset;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.StringUtils;
import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class GuiRenderSettings extends GuiScreen implements GuiReplayOverlay.NoOverlay {

    private final Minecraft mc = Minecraft.getMinecraft();

    //for default file
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    private static final int LEFT_BORDER = 10;

    private GuiAdvancedButton renderButton = new GuiAdvancedButton(0, 0, 100, 20, I18n.format("replaymod.gui.render"), new Runnable() {
        @Override
        public void run() {
            startRendering();
        }
    }, null);

    private GuiAdvancedButton cancelButton = new GuiAdvancedButton(0, 0, 100, 20, I18n.format("replaymod.gui.cancel"), new Runnable() {
        @Override
        public void run() {
            mc.displayGuiScreen(null);
        }
    }, null);

    private GuiAdvancedButton toggleTabButton = new GuiAdvancedButton(0, 0, 150, 20, I18n.format("replaymod.gui.rendersettings.advanced"), new Runnable() {
        @Override
        public void run() {
            toggleTab();
        }
    }, null);

    private GuiNumberInput xRes = new GuiNumberInput(mc.fontRendererObj, 0, 0, 50, 1, 100000, mc.displayWidth, false) {
        @Override
        public void moveCursorBy(int move) {
            super.moveCursorBy(move);
            RendererSettings renderer = rendererDropdown.getElement(rendererDropdown.getSelectionIndex());
            Integer value = getIntValueNullable();
            if (value != null) {
                if (renderer == RendererSettings.CUBIC) {
                    yRes.text = Integer.toString(Math.max(1, value * 3 / 4));
                }
                if (renderer == RendererSettings.EQUIRECTANGULAR) {
                    yRes.text = Integer.toString(Math.max(1, value / 2));
                }
            }
            yRes.setCursorPositionEnd();
            validateInputs();
        }
    };

    private GuiNumberInput yRes = new GuiNumberInput(mc.fontRendererObj, 0, 0, 50, 1, 10000, mc.displayHeight, false) {
        @Override
        public void moveCursorBy(int move) {
            super.moveCursorBy(move);
            RendererSettings renderer = rendererDropdown.getElement(rendererDropdown.getSelectionIndex());
            Integer value = getIntValueNullable();
            if (value != null) {
                if (renderer == RendererSettings.CUBIC) {
                    xRes.text = Integer.toString(value * 4 / 3);
                }
                if (renderer == RendererSettings.EQUIRECTANGULAR) {
                    xRes.text = Integer.toString(value * 2);
                }
            }
            xRes.setCursorPositionEnd();
            validateInputs();
        }
    };

    private GuiString rendererString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.rendersettings.renderer")+":");
    private GuiString presetsString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.rendersettings.presets")+":");
    private GuiString resolutionString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.rendersettings.customresolution")+":");
    private GuiString asteriksString = new GuiString(0, 0, Color.WHITE, "*");
    private GuiString bitrateString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.settings.bitrate")+":");
    private GuiString fileChooserString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.rendersettings.outputfile")+":");

    private GuiDropdown<RendererSettings> rendererDropdown = new GuiDropdown<RendererSettings>(mc.fontRendererObj, 0, 0, 200, 5);
    private GuiDropdown<EncodingPreset> encodingPresetDropdown = new GuiDropdown<EncodingPreset>(mc.fontRendererObj, 0, 1, 200, 5);

    private GuiString stabilizeString = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.rendersettings.stabilizecamera")+":");

    private GuiAdvancedCheckBox stablePitch = new GuiAdvancedCheckBox(I18n.format("replaymod.gui.pitch"), false, false);
    private GuiAdvancedCheckBox stableYaw = new GuiAdvancedCheckBox(I18n.format("replaymod.gui.yaw"), false, false);
    private GuiAdvancedCheckBox stableRoll = new GuiAdvancedCheckBox(I18n.format("replaymod.gui.roll"), false, false);

    private int virtualY, virtualHeight;

    private GuiAdvancedCheckBox enableGreenscreen = new GuiAdvancedCheckBox(0, 0, I18n.format("replaymod.gui.rendersettings.chromakey"), false);
    private GuiAdvancedCheckBox renderNameTags = new GuiAdvancedCheckBox(0, 0, I18n.format("replaymod.gui.rendersettings.nametags"), true);

    {
        enableGreenscreen.addCheckBoxListener(new CheckBoxListener() {
            @Override
            public void onCheck(boolean checked) {
                colorPicker.setElementEnabled(checked);
            }
        });
    }

    private GuiVideoFramerateSlider framerateSlider = new GuiVideoFramerateSlider(0, 0, ReplayMod.replaySettings.getVideoFramerate(),
            I18n.format("replaymod.gui.rendersettings.framerate"));

    private GuiNumberInput bitrateInput = new GuiNumberInputWithText(mc.fontRendererObj, 0, 0, 50, 1D, null, 10000D, false, " kbps");
    private GuiColorPicker colorPicker = new GuiColorPicker(GuiConstants.RENDER_SETTINGS_COLOR_PICKER, 0, 0, I18n.format("replaymod.gui.rendersettings.skycolor")+": ", 0, 0);
    private GuiAdvancedTextField commandInput = new GuiAdvancedTextField(mc.fontRendererObj, I18n.format("replaymod.gui.rendersettings.command"), 3000);
    private GuiAdvancedTextField ffmpegArguments = new GuiAdvancedTextField(mc.fontRendererObj, I18n.format("replaymod.gui.rendersettings.ffmpeghint"), 3000);

    private File defaultFile = null;
    {
        try {
            defaultFile = new File(ReplayFileIO.getRenderFolder(), FILE_FORMAT.format(Calendar.getInstance().getTime())+"."+ EncodingPreset.MP4DEFAULT.getFileExtension());
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private GuiFileChooser outputFileChooser = new GuiFileChooser(GuiConstants.RENDER_SETTINGS_OUTPUT_CHOOSER, 0, 0,
            "", defaultFile, new String[]{"webm"}, true);

    {
        Keyboard.enableRepeatEvents(true);

        rendererDropdown.addSelectionListener(new RendererDropdownListener());

        int i = 0;
        for (RendererSettings r : RendererSettings.values()) {
            rendererDropdown.addElement(r);
            rendererDropdown.setHoverText(i, r.getDescription());
            i++;
        }

        encodingPresetDropdown.addSelectionListener(new EncodingDropdownListener());

        for (EncodingPreset preset : EncodingPreset.values()) {
            encodingPresetDropdown.addElement(preset);
        }

        encodingPresetDropdown.setSelectionIndex(2);
    }

    private String[] tabNames = new String[]{I18n.format("replaymod.gui.rendersettings.video"),
        I18n.format("replaymod.gui.rendersettings.advanced"), I18n.format("replaymod.gui.rendersettings.commandline")};

    private int currentTab = 0;

    private void toggleTab() {
        currentTab++;
        if(currentTab >= 3) currentTab = 0;

        int nextTab = currentTab+1;
        if(nextTab >= 3) nextTab = 0;
        toggleTabButton.displayString = tabNames[nextTab];
    }

    private DelegatingElement currentScreen = new DelegatingElement() {

        private ComposedElement videoSettings = new ComposedElement(renderButton, cancelButton, toggleTabButton,
                rendererString, rendererDropdown, presetsString, encodingPresetDropdown, resolutionString, asteriksString,
                xRes, yRes, framerateSlider, bitrateString, bitrateInput, fileChooserString, outputFileChooser);

        private ComposedElement advancedSettings = new ComposedElement(renderButton, cancelButton, toggleTabButton,
                renderNameTags, stabilizeString, stablePitch, stableYaw, stableRoll, enableGreenscreen, colorPicker);

        private ComposedElement commandLineSettings = new ComposedElement(renderButton, cancelButton, toggleTabButton,
                commandInput, ffmpegArguments);

        @Override
        public GuiElement delegate() {
            switch(currentTab) {
                case 0:
                    return videoSettings;
                case 1:
                    return advancedSettings;
                case 2:
                    return commandLineSettings;
                default:
                    return null;
            }
        }
    };

    @Override
    public void initGui() {
        virtualHeight = 200;
        virtualY = (this.height-virtualHeight)/2;

        cancelButton.xPosition = width-LEFT_BORDER-5-100;
        renderButton.xPosition = cancelButton.xPosition-5-100;
        toggleTabButton.xPosition = renderButton.xPosition-5-150;

        cancelButton.yPosition = renderButton.yPosition = toggleTabButton.yPosition = virtualY+virtualHeight-5-20;

        initializeVideoTab();
        initializeAdvancedTab();
        initializeCommandLineTab();

        validateInputs();
    }

    private void initializeVideoTab() {
        int largerWidth = Math.max(Math.max(rendererString.getWidth(), presetsString.getWidth()),
                resolutionString.getWidth())+10;
        int rowWidth = largerWidth+rendererDropdown.getWidth();
        rendererDropdown.xPosition = encodingPresetDropdown.xPosition = xRes.xPosition =
                bitrateInput.xPosition = outputFileChooser.xPosition = (width - rowWidth)/2 + largerWidth;

        rendererString.positionX = presetsString.positionX = fileChooserString.positionX =
                resolutionString.positionX = bitrateString.positionX = (width - rowWidth)/2;

        rendererString.positionX += (largerWidth-10)-rendererString.getWidth();
        presetsString.positionX += (largerWidth-10)-presetsString.getWidth();
        fileChooserString.positionX += (largerWidth-10)-fileChooserString.getWidth();
        resolutionString.positionX += (largerWidth-10)-resolutionString.getWidth();
        bitrateString.positionX += (largerWidth-10)-bitrateString.getWidth();

        rendererDropdown.yPosition = virtualY + 25;
        rendererString.positionY = rendererDropdown.yPosition + 6;

        encodingPresetDropdown.yPosition = rendererDropdown.yPosition+20+10;
        presetsString.positionY = encodingPresetDropdown.yPosition + 6;

        asteriksString.positionX = xRes.xPosition+xRes.width+5;
        yRes.xPosition = asteriksString.positionX+asteriksString.getWidth()+5;

        xRes.yPosition = yRes.yPosition = encodingPresetDropdown.yPosition+20+10;
        resolutionString.positionY = asteriksString.positionY = xRes.yPosition + 6;

        bitrateInput.width = 70;

        framerateSlider.xPosition = bitrateInput.xPosition + bitrateInput.width + 5;
        framerateSlider.width = 125;

        bitrateInput.yPosition = framerateSlider.yPosition = xRes.yPosition+20+10;
        bitrateString.positionY = bitrateInput.yPosition+6;

        outputFileChooser.yPosition = bitrateInput.yPosition+20+10;
        fileChooserString.positionY = outputFileChooser.yPosition+6;

        outputFileChooser.width = 200;
    }

    private void initializeAdvancedTab() {
        int singleWidth = 150;
        int middleGap = 10;

        int totalWidth = (2*singleWidth)+middleGap;

        int leftX = width/2 - (singleWidth+middleGap/2);

        //might be of use in the future
        //int rightX = width/2 + (middleGap/2);

        int heightDiff = 25;

        renderNameTags.xPosition = leftX;
        renderNameTags.yPosition = virtualY + 25;
        renderNameTags.width = singleWidth;

        stabilizeString.positionX = leftX;

        int left = leftX+stabilizeString.getWidth()+middleGap;
        int width = (totalWidth - (stabilizeString.getWidth()+middleGap)) / 3;

        stableYaw.xPosition = left;
        stablePitch.xPosition = left + width;
        stableRoll.xPosition = left + 2*width;

        stablePitch.yPosition = stableYaw.yPosition = stableRoll.yPosition = renderNameTags.yPosition+heightDiff;
        stabilizeString.positionY = stablePitch.yPosition + 2;

        enableGreenscreen.xPosition = leftX;
        colorPicker.xPosition = leftX + (enableGreenscreen.width+5);
        colorPicker.width = totalWidth - (enableGreenscreen.width+5);

        colorPicker.yPosition = stablePitch.yPosition+heightDiff;
        enableGreenscreen.yPosition = colorPicker.yPosition+4;

        colorPicker.pickerX = colorPicker.xPosition;
        colorPicker.pickerY = colorPicker.yPosition + 20;

        colorPicker.setElementEnabled(enableGreenscreen.isChecked());
    }

    private void initializeCommandLineTab() {
        commandInput.width = 55;
        commandInput.xPosition = (this.width-305)/2;
        commandInput.yPosition = ffmpegArguments.yPosition = virtualY + 25;

        ffmpegArguments.width = 245;
        ffmpegArguments.xPosition = commandInput.xPosition+commandInput.width+5;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(LEFT_BORDER, virtualY, width - LEFT_BORDER, virtualY + virtualHeight, -1072689136, -804253680);

        this.drawCenteredString(fontRendererObj, tabNames[currentTab],
                this.width / 2, virtualY + 5, Color.WHITE.getRGB());

        if(currentTab == 2) {
            String[] rows = StringUtils.splitStringInMultipleRows(I18n.format("replaymod.gui.rendersettings.ffmpeg.description"), 305);

            int i = 0;
            for(String row : rows) {
                drawString(fontRendererObj, row, commandInput.xPosition, commandInput.yPosition + 30 + (15 * i), Color.WHITE.getRGB());
                i++;
            }
        }

        currentScreen.draw(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        currentScreen.mouseClick(mc, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        currentScreen.mouseRelease(mc, mouseX, mouseX, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        currentScreen.mouseDrag(mc, mouseX, mouseY, clickedMouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Point mousePos = MouseUtils.getMousePos();
        currentScreen.buttonPressed(mc, mousePos.getX(), mousePos.getY(), typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        xRes.updateCursorCounter();
        yRes.updateCursorCounter();
        bitrateInput.updateCursorCounter();
        commandInput.updateCursorCounter();
        ffmpegArguments.updateCursorCounter();
        super.updateScreen();
    }

    private enum RendererSettings implements GuiEntryListEntry {
        DEFAULT("default"),
        STEREOSCOPIC("stereoscopic"),
        CUBIC("cubic"),
        EQUIRECTANGULAR("equirectangular");

        private String name, desc;

        RendererSettings(String name) {
            this.name = "replaymod.gui.rendersettings.renderer."+name;
            this.desc = this.name+".description";
        }

        @Override
        public String toString() {
            return I18n.format(name);
        }

        public String getDescription() { return I18n.format(desc); }

        @Override
        public String getDisplayString() {
            return toString();
        }
    }

    private void startRendering() {
        RendererSettings r = rendererDropdown.getElement(rendererDropdown.getSelectionIndex());

        RenderOptions options = new RenderOptions();

        options.setLinearMovement(ReplayMod.replaySettings.isLinearMovement());

        options.setWaitForChunks(true);

        options.setFps(framerateSlider.getFPS());
        ReplayMod.replaySettings.setVideoFramerate(framerateSlider.getFPS());

        options.setBitrate(bitrateInput.getIntValue() + "K"); //Bitrate value is in Kilobytes

        //remove the extension from the output file
        File outputFile = outputFileChooser.getSelectedFile();
        outputFile = new File(outputFile.getParent(), FilenameUtils.getBaseName(outputFile.getAbsolutePath()));

        options.setOutputFile(outputFile);

        if(enableGreenscreen.isChecked()) {
            options.setSkyColor(colorPicker.getPickedColor());
        }

        options.setHideNameTags(!renderNameTags.isChecked());

        options.setWidth(getWidthSetting());
        options.setHeight(getHeightSetting());

        Pipelines.Preset pipePreset = Pipelines.Preset.DEFAULT;
        if(r == RendererSettings.DEFAULT) {
            pipePreset = Pipelines.Preset.DEFAULT;
        } else if(r == RendererSettings.STEREOSCOPIC) {
            pipePreset = Pipelines.Preset.STEREOSCOPIC;
        } else if(r == RendererSettings.CUBIC) {
            pipePreset = Pipelines.Preset.CUBIC;
        } else if(r == RendererSettings.EQUIRECTANGULAR) {
            pipePreset = Pipelines.Preset.EQUIRECTANGULAR;
        }
        options.setMode(pipePreset);

        options.setIgnoreCameraRotation(stableYaw.enabled && stableYaw.isChecked(),
                stablePitch.enabled && stablePitch.isChecked(), stableRoll.enabled && stableRoll.isChecked());

        if (isCtrlKeyDown()) {
            options.setHighPerformance(true);
        }

        if(commandInput.getText().trim().length() > 0) {
            options.setExportCommand(commandInput.getText().trim());
        }

        if(ffmpegArguments.getText().trim().length() > 0) {
            options.setExportCommandArgs(ffmpegArguments.getText().trim());
        }

        if(FMLClientHandler.instance().hasOptifine()) {
            mc.displayGuiScreen(new GuiErrorScreen(I18n.format("replaymod.gui.rendering.error.title"), I18n.format("replaymod.gui.rendering.error.optifine")));
        } else {
            ReplayHandler.startPath(options);
        }

    }

    private int getWidthSetting() {
        return xRes.getIntValue();
    }

    private int getHeightSetting() { return yRes.getIntValue(); }

    private class RendererDropdownListener implements SelectionListener {
        @Override
        public void onSelectionChanged(int selectionIndex) {
            xRes.setCursorPositionEnd();
            xRes.setSelectionPos(xRes.getCursorPosition());

            yRes.setCursorPositionEnd();
            yRes.setSelectionPos(yRes.getCursorPosition());

            yRes.moveCursorBy(0); //This causes the Aspect Ratio to be recalculated based on the Y Resolution

            validateInputs();
        }
    }

    private class EncodingDropdownListener implements SelectionListener {
        @Override
        public void onSelectionChanged(int selectionIndex) {
            EncodingPreset preset = encodingPresetDropdown.getElement(selectionIndex);

            bitrateInput.setEnabled(preset.hasBitrateSetting());
            ffmpegArguments.setText(preset.getCommandLineArgs());
            ffmpegArguments.setCursorPositionZero();

            outputFileChooser.setAllowedExtensions(new String[]{preset.getFileExtension()});

            File selectedFile = outputFileChooser.getSelectedFile();
            if(selectedFile != null) {
                String newName = FilenameUtils.getBaseName(selectedFile.getAbsolutePath())+"."+preset.getFileExtension();
                outputFileChooser.setSelectedFile(new File(selectedFile.getParent(), newName));
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private void validateInputs() {
        boolean valid = true;

        boolean isPreset = false;
        EncodingPreset curPreset = encodingPresetDropdown.getElement(encodingPresetDropdown.getSelectionIndex());
        if(ffmpegArguments.getText().trim().equals(curPreset.getCommandLineArgs())) {
            isPreset = true;
        }

        if(isPreset) {
            if(curPreset.isYuv420()) {
                if(getWidthSetting() % 2 != 0 || getHeightSetting() % 2 != 0) {
                    valid = false;
                    renderButton.hoverText = I18n.format("replaymod.gui.rendersettings.customresolution.warning.yuv420");
                }
            }
        }

        boolean isPanoramic = false;

        switch (rendererDropdown.getElement(rendererDropdown.getSelectionIndex())) {
            case CUBIC:
                isPanoramic = true;
                if (getWidthSetting() * 3 / 4 != getHeightSetting()
                        || getWidthSetting() * 3 % 4 != 0) {
                    valid = false;
                    renderButton.hoverText = I18n.format("replaymod.gui.rendersettings.customresolution.warning.cubic");
                }
                break;
            case EQUIRECTANGULAR:
                isPanoramic = true;
                if (getWidthSetting() / 2 != getHeightSetting()
                        || getWidthSetting() % 2 != 0) {
                    valid = false;
                    renderButton.hoverText = I18n.format("replaymod.gui.rendersettings.customresolution.warning.equirectangular");
                }
                break;
        }

        stabilizeString.setElementEnabled(isPanoramic);
        stableYaw.setElementEnabled(isPanoramic);
        stablePitch.setElementEnabled(isPanoramic);
        stableRoll.setElementEnabled(isPanoramic);

        if (valid) {
            renderButton.enabled = true;
            renderButton.hoverText = "";
            xRes.setTextColor(0xffffffff);
            yRes.setTextColor(0xffffffff);
            xRes.setDisabledTextColour(0xff707070);
            yRes.setDisabledTextColour(0xff707070);
        } else {
            renderButton.enabled = false;
            xRes.setTextColor(0xffff0000);
            yRes.setTextColor(0xffff0000);
            xRes.setDisabledTextColour(0xffff0000);
            yRes.setDisabledTextColour(0xffff0000);
        }
    }
}
