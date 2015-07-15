package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.EncodingPreset;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.StringUtils;
import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GuiRenderSettings extends GuiScreen {

    //for default file
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    private static final int LEFT_BORDER = 10;

    private GuiAdvancedButton renderButton, cancelButton, advancedButton;
    private GuiDropdown<RendererSettings> rendererDropdown;
    private GuiDropdown<EncodingPreset> encodingPresetDropdown;

    private int virtualY, virtualHeight;

    private GuiCheckBox ignoreCamDir, enableGreenscreen;
    private GuiNumberInput xRes, yRes;
    private GuiToggleButton interpolation, forceChunks;
    private GuiVideoFramerateSlider framerateSlider;
    private GuiNumberInput bitrateInput;
    private GuiColorPicker colorPicker;
    private GuiAdvancedTextField commandInput, ffmpegArguments;
    private GuiFileChooser outputFileChooser;

    private List<GuiButton> permanentButtons = new ArrayList<GuiButton>();
    private List<GuiButton> defaultButtons = new ArrayList<GuiButton>();
    private List<GuiButton> advancedButtons = new ArrayList<GuiButton>();

    private boolean advancedTab = false;

    private int w1;

    private boolean initialized;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void initGui() {
        if(!initialized) {
            Keyboard.enableRepeatEvents(true);

            rendererDropdown = new GuiDropdown<RendererSettings>(fontRendererObj, 0, 0, 200, 5);
            rendererDropdown.addSelectionListener(new RendererDropdownListener());

            int i = 0;
            for (RendererSettings r : RendererSettings.values()) {
                rendererDropdown.addElement(r);
                rendererDropdown.setHoverText(i, r.getDescription());
                i++;
            }

            encodingPresetDropdown = new GuiDropdown<EncodingPreset>(fontRendererObj, 0, 0, 200, 5);
            encodingPresetDropdown.addSelectionListener(new EncodingDropdownListener());

            for (EncodingPreset preset : EncodingPreset.values()) {
                encodingPresetDropdown.addElement(preset);
            }

            renderButton = new GuiAdvancedButton(GuiConstants.RENDER_SETTINGS_RENDER_BUTTON, 0, 0, I18n.format("replaymod.gui.render"));
            cancelButton = new GuiAdvancedButton(GuiConstants.RENDER_SETTINGS_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.cancel"));
            advancedButton = new GuiAdvancedButton(GuiConstants.RENDER_SETTINGS_ADVANCED_BUTTON, 0, 0, I18n.format("replaymod.gui.rendersettings.advanced"));

            xRes = new GuiNumberInput(fontRendererObj, 0, 0, 50, 1, 100000, mc.displayWidth, false) {
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
            yRes = new GuiNumberInput(fontRendererObj, 0, 0, 50, 1, 10000, mc.displayHeight, false) {
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

            bitrateInput = new GuiNumberInputWithText(fontRendererObj, 0, 0, 50, 1D, null, 10000D, false, " kbps");

            bitrateInput.setElementEnabled(true);

            framerateSlider = new GuiVideoFramerateSlider(GuiConstants.RENDER_SETTINGS_FRAMERATE_SLIDER, 0, 0, ReplayMod.replaySettings.getVideoFramerate(),
                    I18n.format("replaymod.gui.rendersettings.framerate"));

            File defaultFile = null;
            try {
                defaultFile = new File(ReplayFileIO.getRenderFolder(), FILE_FORMAT.format(Calendar.getInstance().getTime())+"."+ EncodingPreset.MP4DEFAULT.getFileExtension());
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }

            outputFileChooser = new GuiFileChooser(GuiConstants.RENDER_SETTINGS_OUTPUT_CHOOSER, 0, 0, I18n.format("replaymod.gui.rendersettings.outputfile")+": ", defaultFile, new String[]{"webm"}, true);

            interpolation = new GuiToggleButton(GuiConstants.RENDER_SETTINGS_INTERPOLATION_BUTTON, 0, 0,
                    I18n.format("replaymod.gui.rendersettings.interpolation")+": ",
                    new String[]{I18n.format("replaymod.gui.settings.interpolation.cubic"),
                            I18n.format("replaymod.gui.settings.interpolation.linear")});

            interpolation.setValue(ReplayMod.replaySettings.isLinearMovement() ? 1 : 0);

            forceChunks = new GuiToggleButton(GuiConstants.RENDER_SETTINGS_FORCECHUNKS_BUTTON, 0, 0,
                    I18n.format("replaymod.gui.rendersettings.forcechunks")+": ",
                    new String[]{I18n.format("options.on"), I18n.format("options.off")});

            forceChunks.setValue(ReplayMod.replaySettings.getWaitForChunks() ? 0 : 1);

            forceChunks.width = interpolation.width = framerateSlider.width = 150;

            enableGreenscreen = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_ENABLE_GREENSCREEN, 0, 0, I18n.format("replaymod.gui.rendersettings.chromakey"), false);

            colorPicker = new GuiColorPicker(GuiConstants.RENDER_SETTINGS_COLOR_PICKER, 0, 0, I18n.format("replaymod.gui.rendersettings.skycolor")+": ", 0, 0);
            colorPicker.enabled = enableGreenscreen.isChecked();

            commandInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 50, 20);
            commandInput.hint = I18n.format("replaymod.gui.rendersettings.command");
            commandInput.setMaxStringLength(3200);

            ffmpegArguments = new GuiAdvancedTextField(fontRendererObj, 0, 0, 50, 20);
            ffmpegArguments.hint = I18n.format("replaymod.gui.rendersettings.ffmpeghint");
            ffmpegArguments.setMaxStringLength(3200);

            ignoreCamDir = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_STATIC_CAMERA, 0, 0, I18n.format("replaymod.gui.rendersettings.stablecamera"), false);
            ignoreCamDir.enabled = false;

            encodingPresetDropdown.setSelectionIndex(2);

            permanentButtons.add(advancedButton);
            permanentButtons.add(renderButton);
            permanentButtons.add(cancelButton);

            defaultButtons.add(framerateSlider);
            defaultButtons.add(ignoreCamDir);
            defaultButtons.add(outputFileChooser);

            advancedButtons.add(interpolation);
            advancedButtons.add(forceChunks);
            advancedButtons.add(enableGreenscreen);
            advancedButtons.add(colorPicker);
        }

        virtualHeight = 200;
        virtualY = (this.height-virtualHeight)/2;

        cancelButton.width = renderButton.width = advancedButton.width = 100;

        cancelButton.xPosition = width-10-5-100;
        renderButton.xPosition = cancelButton.xPosition-5-100;
        advancedButton.xPosition = renderButton.xPosition-5-100;

        cancelButton.yPosition = renderButton.yPosition = advancedButton.yPosition = virtualY+virtualHeight-5-18;

        w1 = rendererDropdown.width + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.rendersettings.renderer") + ":")+10;
        rendererDropdown.yPosition = virtualY + 15 + 15;
        rendererDropdown.xPosition = encodingPresetDropdown.xPosition =
                (width-w1)/2 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.rendersettings.renderer") + ":")+10;

        encodingPresetDropdown.yPosition = rendererDropdown.yPosition + 20 + 10;

        String customResString = I18n.format("replaymod.gui.rendersettings.customresolution");
        int customResWidth = fontRendererObj.getStringWidth(customResString);

        int w2 = customResWidth + 5 + xRes.width + 5 + fontRendererObj.getStringWidth("*") + 5 + yRes.width;

        int yPos = virtualY + 15 + 5 + 50 + 10 + 5+fontRendererObj.getStringWidth("*")+5;
        int xPos = (width- w2)/2;

        xRes.xPosition = xPos + customResWidth + 5;
        yRes.xPosition = xRes.xPosition+xRes.width+5+fontRendererObj.getStringWidth("*")+5;
        xRes.yPosition = yRes.yPosition = yPos-3;

        int w3 = interpolation.width + 10 + forceChunks.width;

        interpolation.xPosition = (width- w3)/2;
        interpolation.yPosition = xRes.yPosition+20+10;

        forceChunks.xPosition = interpolation.xPosition+interpolation.width+10;
        forceChunks.yPosition = interpolation.yPosition;

        String bitrateString = I18n.format("replaymod.gui.settings.bitrate")+": ";
        int bsw = fontRendererObj.getStringWidth(bitrateString);
        bitrateInput.xPosition = forceChunks.xPosition + bsw;
        bitrateInput.width = forceChunks.width - bsw;

        framerateSlider.xPosition = outputFileChooser.xPosition = interpolation.xPosition;
        framerateSlider.yPosition = bitrateInput.yPosition = xRes.yPosition + 20 + 10;

        ignoreCamDir.xPosition = framerateSlider.xPosition + (framerateSlider.width - ignoreCamDir.width)/2;

        ignoreCamDir.yPosition = framerateSlider.yPosition+20+10;

        outputFileChooser.yPosition = framerateSlider.yPosition+20+10;
        outputFileChooser.width = 312;

        //align all advanced buttons

        int i = 0;
        for(GuiButton b : advancedButtons) {
            b.width = 150;
            b.xPosition = i % 2 == 0 ? this.width/2 - b.width - 2 : this.width/2 + 2;

            b.yPosition = this.virtualY + 20 + ((i/2)*25);

            if(b instanceof GuiColorPicker) {
                GuiColorPicker picker = (GuiColorPicker)b;
                picker.pickerX = b.xPosition + 25;
                picker.pickerY = b.yPosition + 20 + 5;
            } else if(b instanceof GuiCheckBox) {
                b.yPosition += 5;
            }

            i++;
        }

        commandInput.width = 55;
        commandInput.xPosition = (this.width-305)/2;
        commandInput.yPosition = ffmpegArguments.yPosition = this.virtualY + 20 + ((i/2)*25);

        ffmpegArguments.width = 245;
        ffmpegArguments.xPosition = commandInput.xPosition+commandInput.width+5;

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(LEFT_BORDER, virtualY, width - LEFT_BORDER, virtualY + virtualHeight, -1072689136, -804253680);

        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.rendersettings.title"),
                this.width / 2, virtualY + 5, Color.WHITE.getRGB());


        List<GuiButton> toHandle = new ArrayList<GuiButton>();
        toHandle.addAll(permanentButtons);
        toHandle.addAll(advancedTab ? advancedButtons : defaultButtons);

        for(GuiButton b : toHandle) {
            b.drawButton(mc, mouseX, mouseY);
        }

        if(!advancedTab) {
            this.drawString(fontRendererObj, I18n.format("replaymod.gui.rendersettings.renderer") + ":",
                    (width - w1) / 2, rendererDropdown.yPosition + 6, Color.WHITE.getRGB());

            this.drawString(fontRendererObj, I18n.format("replaymod.gui.rendersettings.presets") + ":",
                    (width - w1) / 2, encodingPresetDropdown.yPosition + 6, Color.WHITE.getRGB());

            String resString = I18n.format("replaymod.gui.rendersettings.customresolution")+":";
            int strWidth = fontRendererObj.getStringWidth(resString);

            this.drawString(fontRendererObj, resString,
                    xRes.xPosition - strWidth - 5, xRes.yPosition+6, Color.WHITE.getRGB());

            xRes.drawTextBox();
            yRes.drawTextBox();
            bitrateInput.drawTextBox();
            this.drawString(fontRendererObj, "*", xRes.xPosition + xRes.width + 5, xRes.yPosition + 6, Color.WHITE.getRGB());

            String bitrateString = I18n.format("replaymod.gui.settings.bitrate")+": ";
            this.drawString(fontRendererObj, bitrateString, forceChunks.xPosition, bitrateInput.yPosition + 6, Color.WHITE.getRGB());

            encodingPresetDropdown.drawTextBox();
            rendererDropdown.drawTextBox();
        } else {
            commandInput.drawTextBox();
            ffmpegArguments.drawTextBox();
            String[] rows = StringUtils.splitStringInMultipleRows(I18n.format("replaymod.gui.rendersettings.ffmpeg.description"), 305);

            int i = 0;
            for(String row : rows) {
                drawString(fontRendererObj, row, commandInput.xPosition, commandInput.yPosition + 30 + (15 * i), Color.WHITE.getRGB());
                i++;
            }
        }

        renderButton.drawOverlay(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(!rendererDropdown.mouseClickedResult(mouseX, mouseY)) {
            if(!encodingPresetDropdown.mouseClickedResult(mouseX, mouseY)) {
                if(!advancedTab) {
                    xRes.mouseClicked(mouseX, mouseY, mouseButton);
                    yRes.mouseClicked(mouseX, mouseY, mouseButton);
                    bitrateInput.mouseClicked(mouseX, mouseY, mouseButton);
                } else {
                    commandInput.mouseClicked(mouseX, mouseY, mouseButton);
                    ffmpegArguments.mouseClicked(mouseX, mouseY, mouseButton);
                }

                if(mouseButton == 0) {
                    List<GuiButton> toHandle = new ArrayList<GuiButton>();
                    toHandle.addAll(permanentButtons);
                    toHandle.addAll(advancedTab ? advancedButtons : defaultButtons);

                    for(GuiButton b : toHandle) {
                        b.mousePressed(this.mc, mouseX, mouseY);
                        if(b.mousePressed(this.mc, mouseX, mouseY)) {
                            b.playPressSound(this.mc.getSoundHandler());
                            actionPerformed(b);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        framerateSlider.mouseReleased(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(advancedTab) colorPicker.mouseDragged(mc, mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(!advancedTab) {
            if(keyCode == Keyboard.KEY_TAB) {
                if(xRes.isFocused()) {
                    xRes.setFocused(false);
                    yRes.setFocused(true);
                } else if(yRes.isFocused()) {
                    yRes.setFocused(false);
                    xRes.setFocused(true);
                }
                return;
            }
            xRes.textboxKeyTyped(typedChar, keyCode);
            yRes.textboxKeyTyped(typedChar, keyCode);
            bitrateInput.textboxKeyTyped(typedChar, keyCode);
        } else {
            commandInput.textboxKeyTyped(typedChar, keyCode);
            ffmpegArguments.textboxKeyTyped(typedChar, keyCode);
        }
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

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;

        if(permanentButtons.contains(button)) {
            if(button instanceof GuiCheckBox)
                ((GuiCheckBox)button).setIsChecked(!((GuiCheckBox)button).isChecked());

            if(button.id == GuiConstants.RENDER_SETTINGS_RENDER_BUTTON) {
                startRendering();
            } else if(button.id == GuiConstants.RENDER_SETTINGS_CANCEL_BUTTON) {
                mc.displayGuiScreen(null);
            } else if(button.id == GuiConstants.RENDER_SETTINGS_ADVANCED_BUTTON) {
                advancedTab = !advancedTab;
                advancedButton.displayString = advancedTab ? I18n.format("replaymod.gui.rendersettings.basic")
                        : I18n.format("replaymod.gui.rendersettings.advanced");
            }

        } else if(defaultButtons.contains(button) && !advancedTab) {
            if(button instanceof GuiCheckBox)
                ((GuiCheckBox)button).setIsChecked(!((GuiCheckBox)button).isChecked());
            else if(button.id == GuiConstants.RENDER_SETTINGS_OUTPUT_CHOOSER) {
                Point mouse = MouseUtils.getMousePos();
                outputFileChooser.mouseClick(mc, mouse.getX(), mouse.getY(), 0);
            }

        } else if(advancedButtons.contains(button) && advancedTab) {
            if(button instanceof GuiCheckBox)
                ((GuiCheckBox)button).setIsChecked(!((GuiCheckBox)button).isChecked());

            if(button instanceof GuiColorPicker) {
                ((GuiColorPicker) button).pickerToggled();
            } else if(button instanceof GuiToggleButton) {
                ((GuiToggleButton) button).toggle();
            } else {
                if(button.id == GuiConstants.RENDER_SETTINGS_ENABLE_GREENSCREEN) {
                    colorPicker.enabled = enableGreenscreen.isChecked();
                }
            }

        }
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

        options.setLinearMovement(interpolation.getValue() == 1);
        ReplayMod.replaySettings.setLinearMovement(interpolation.getValue() == 1);

        options.setWaitForChunks(forceChunks.getValue() == 0);
        ReplayMod.replaySettings.setWaitForChunks(forceChunks.getValue() == 0);

        options.setFps(framerateSlider.getFPS());
        ReplayMod.replaySettings.setVideoFramerate(framerateSlider.getFPS());

        options.setBitrate(bitrateInput.getIntValue() + "K"); //Bitrate value is in Kilobytes

        //remove the extension from the output file
        File outputFile = outputFileChooser.getSelectedFile();
        outputFile = new File(outputFile.getParent(), FilenameUtils.getBaseName(outputFile.getAbsolutePath()));

        if(outputFile.exists()) FileUtils.deleteQuietly(outputFile);
        options.setOutputFile(outputFile);

        if(enableGreenscreen.isChecked()) {
            options.setSkyColor(colorPicker.getPickedColor());
        }

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

        options.setIgnoreCameraRotation(ignoreCamDir.isChecked());

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
            RendererSettings s = rendererDropdown.getElement(selectionIndex);

            if(s == RendererSettings.DEFAULT) {
                ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.STEREOSCOPIC) {
                ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.CUBIC) {
                ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.EQUIRECTANGULAR) {
                ignoreCamDir.enabled = true;
            }

            if (!ignoreCamDir.enabled) {
                ignoreCamDir.setIsChecked(false);
            }

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
        switch (rendererDropdown.getElement(rendererDropdown.getSelectionIndex())) {
            case CUBIC:
                if (getWidthSetting() * 3 / 4 != getHeightSetting()
                        || getWidthSetting() * 3 % 4 != 0) {
                    valid = false;
                    renderButton.hoverText = I18n.format("replaymod.gui.rendersettings.customresolution.warning.cubic");
                }
                break;
            case EQUIRECTANGULAR:
                if (getWidthSetting() / 2 != getHeightSetting()
                        || getWidthSetting() % 2 != 0) {
                    valid = false;
                    renderButton.hoverText = I18n.format("replaymod.gui.rendersettings.customresolution.warning.equirectangular");
                }
                break;
        }
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
