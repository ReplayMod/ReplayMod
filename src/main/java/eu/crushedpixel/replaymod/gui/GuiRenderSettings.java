package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.GuiColorPicker;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
import eu.crushedpixel.replaymod.gui.elements.GuiToggleButton;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.frame.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiRenderSettings extends GuiScreen {
    private static final int LEFT_BORDER = 10;

    private GuiButton renderButton, cancelButton, advancedButton;
    private GuiDropdown<RendererSettings> rendererDropdown;

    private int virtualY, virtualHeight;

    private GuiCheckBox customResolution, ignoreCamDir, youtubeExport, enableGreenscreen;
    private GuiNumberInput xRes, yRes;
    private GuiToggleButton interpolation, forceChunks;
    private GuiVideoFramerateSlider framerateSlider;
    private GuiVideoQualitySlider qualitySlider;
    private GuiColorPicker colorPicker;

    private List<GuiButton> permanentButtons = new ArrayList<GuiButton>();
    private List<GuiButton> defaultButtons = new ArrayList<GuiButton>();
    private List<GuiButton> advancedButtons = new ArrayList<GuiButton>();

    private boolean advancedTab = false;

    private int w1;

    private boolean initialized;

    private final Minecraft mc = Minecraft.getMinecraft();

    public GuiRenderSettings() {
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        if(!initialized) {
            rendererDropdown = new GuiDropdown<RendererSettings>(GuiConstants.RENDER_SETTINGS_RENDERER_DROPDOWN,
                    fontRendererObj, 0, 0, 200, 5);
            rendererDropdown.addSelectionListener(new RendererDropdownListener());

            int i = 0;
            for (RendererSettings r : RendererSettings.values()) {
                rendererDropdown.addElement(r);
                rendererDropdown.setHoverText(i, r.getDescription());
                i++;
            }

            renderButton = new GuiButton(GuiConstants.RENDER_SETTINGS_RENDER_BUTTON, 0, 0, I18n.format("replaymod.gui.render"));
            cancelButton = new GuiButton(GuiConstants.RENDER_SETTINGS_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.cancel"));
            advancedButton = new GuiButton(GuiConstants.RENDER_SETTINGS_ADVANCED_BUTTON, 0, 0, I18n.format("replaymod.gui.rendersettings.advanced"));

            customResolution = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_RESOLUTION_CHECKBOX, 0, 0, I18n.format("replaymod.gui.rendersettings.customresolution"), false);

            xRes = new GuiNumberInput(GuiConstants.RENDER_SETTINGS_RESOLUTION_X, fontRendererObj, 0, 0, 50, 1, 100000, mc.displayWidth, false) {
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
                }
            };
            yRes = new GuiNumberInput(GuiConstants.RENDER_SETTINGS_RESOLUTION_Y, fontRendererObj, 0, 0, 50, 1, 100000, mc.displayHeight, false) {
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
                }
            };

            xRes.setEnabled(false);
            yRes.setEnabled(false);

            framerateSlider = new GuiVideoFramerateSlider(GuiConstants.RENDER_SETTINGS_FRAMERATE_SLIDER, 0, 0, ReplayMod.replaySettings.getVideoFramerate(),
                    I18n.format("replaymod.gui.rendersettings.framerate"));
            qualitySlider = new GuiVideoQualitySlider(GuiConstants.RENDER_SETTINGS_QUALITY_SLIDER, 0, 0, (float)ReplayMod.replaySettings.getVideoQuality(),
                    I18n.format("replaymod.gui.rendersettings.quality"));

            interpolation = new GuiToggleButton(GuiConstants.RENDER_SETTINGS_INTERPOLATION_BUTTON, 0, 0,
                    I18n.format("replaymod.gui.rendersettings.interpolation")+": ",
                    new String[]{I18n.format("replaymod.gui.settings.interpolation.cubic"),
                            I18n.format("replaymod.gui.settings.interpolation.linear")});

            interpolation.setValue(ReplayMod.replaySettings.isLinearMovement() ? 1 : 0);

            forceChunks = new GuiToggleButton(GuiConstants.RENDER_SETTINGS_FORCECHUNKS_BUTTON, 0, 0,
                    I18n.format("replaymod.gui.rendersettings.forcechunks")+": ",
                    new String[]{I18n.format("options.on"), I18n.format("options.off")});

            forceChunks.setValue(ReplayMod.replaySettings.getWaitForChunks() ? 0 : 1);

            forceChunks.width = interpolation.width = framerateSlider.width = qualitySlider.width = 150;

            enableGreenscreen = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_ENABLE_GREENSCREEN, 0, 0, I18n.format("replaymod.gui.rendersettings.chromakey"), false);

            colorPicker = new GuiColorPicker(GuiConstants.RENDER_SETTINGS_COLOR_PICKER, 0, 0, I18n.format("replaymod.gui.rendersettings.skycolor")+": ", 0, 0);
            colorPicker.enabled = enableGreenscreen.isChecked();

            ignoreCamDir = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_STATIC_CAMERA, 0, 0, I18n.format("replaymod.gui.rendersettings.stablecamera"), false);
            youtubeExport = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_YOUTUBE_READY, 0, 0, I18n.format("replaymod.gui.rendersettings.exportyoutube"), true);
            ignoreCamDir.enabled = youtubeExport.enabled = false;

            permanentButtons.add(advancedButton);
            permanentButtons.add(renderButton);
            permanentButtons.add(cancelButton);

            defaultButtons.add(customResolution);
            defaultButtons.add(framerateSlider);
            defaultButtons.add(qualitySlider);
            defaultButtons.add(ignoreCamDir);
            defaultButtons.add(youtubeExport);

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
        rendererDropdown.xPosition = (width-w1)/2 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.rendersettings.renderer") + ":")+10;

        int w2 = customResolution.width + 5 + xRes.width + 5 + fontRendererObj.getStringWidth("*") + 5 + yRes.width;

        customResolution.yPosition = virtualY + 15 + 5 + 20 + 10 +5+fontRendererObj.getStringWidth("*")+5;
        customResolution.xPosition = (width- w2)/2;

        xRes.xPosition = customResolution.xPosition + customResolution.width + 5;
        yRes.xPosition = xRes.xPosition+xRes.width+5+fontRendererObj.getStringWidth("*")+5;
        xRes.yPosition = yRes.yPosition = customResolution.yPosition-3;

        int w3 = interpolation.width + 10 + forceChunks.width;

        interpolation.xPosition = (width- w3)/2;
        interpolation.yPosition = xRes.yPosition+20+10;

        forceChunks.xPosition = interpolation.xPosition+interpolation.width+10;
        forceChunks.yPosition = interpolation.yPosition;

        framerateSlider.xPosition = interpolation.xPosition;
        qualitySlider.xPosition = forceChunks.xPosition;
        framerateSlider.yPosition = qualitySlider.yPosition = interpolation.yPosition + 20 + 10;

        ignoreCamDir.xPosition = framerateSlider.xPosition + (framerateSlider.width - ignoreCamDir.width)/2;
        youtubeExport.xPosition = qualitySlider.xPosition + (qualitySlider.width - youtubeExport.width)/2;

        ignoreCamDir.yPosition = youtubeExport.yPosition = framerateSlider.yPosition+20+10;

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
                    (width - w1) / 2, rendererDropdown.yPosition + 8, Color.WHITE.getRGB());

            xRes.drawTextBox();
            yRes.drawTextBox();
            this.drawString(fontRendererObj, "*", xRes.xPosition + xRes.width + 5, xRes.yPosition + 3, Color.WHITE.getRGB());

            rendererDropdown.drawTextBox();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(!rendererDropdown.mouseClickedResult(mouseX, mouseY)) {
            xRes.mouseClicked(mouseX, mouseY, mouseButton);
            yRes.mouseClicked(mouseX, mouseY, mouseButton);

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

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        framerateSlider.mouseReleased(mouseX, mouseY);
        qualitySlider.mouseReleased(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(advancedTab) colorPicker.mouseDragged(mc, mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_TAB) {
            if(xRes.isFocused() && customResolution.isChecked()) {
                xRes.setFocused(false);
                yRes.setFocused(true);
            }
            else if(yRes.isFocused() && customResolution.isChecked()) {
                yRes.setFocused(false);
                xRes.setFocused(true);
            }
            return;
        }
        xRes.textboxKeyTyped(typedChar, keyCode);
        yRes.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        xRes.updateCursorCounter();
        yRes.updateCursorCounter();
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
            if(button.id == GuiConstants.RENDER_SETTINGS_RESOLUTION_CHECKBOX) {
                boolean enabled = customResolution.isChecked();
                xRes.setEnabled(enabled);
                yRes.setEnabled(enabled);
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

    private enum RendererSettings {
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
    }

    private void startRendering() {
        FrameRenderer renderer = null;

        RendererSettings r = rendererDropdown.getElement(rendererDropdown.getSelectionIndex());

        RenderOptions options = new RenderOptions();

        options.setLinearMovement(interpolation.getValue() == 1);
        ReplayMod.replaySettings.setLinearMovement(interpolation.getValue() == 1);

        options.setWaitForChunks(forceChunks.getValue() == 0);
        ReplayMod.replaySettings.setWaitForChunks(forceChunks.getValue() == 0);

        options.setFps(framerateSlider.getFPS());
        ReplayMod.replaySettings.setVideoFramerate(framerateSlider.getFPS());

        //TODO options.setQuality(qualitySlider.getQuality());
        ReplayMod.replaySettings.setVideoQuality(qualitySlider.getQuality());

        if(enableGreenscreen.isChecked()) {
            options.setSkyColor(colorPicker.getPickedColor());
        }

        options.setWidth(getWidthSetting());
        options.setHeight(getHeightSetting());

        if(r == RendererSettings.DEFAULT) {
            renderer = new DefaultFrameRenderer(options);
        } else if(r == RendererSettings.STEREOSCOPIC) {
            renderer = new StereoscopicFrameRenderer(options);
        } else if(r == RendererSettings.CUBIC) {
            renderer = new CubicFrameRenderer(options, ignoreCamDir.isChecked());
        } else if(r == RendererSettings.EQUIRECTANGULAR) {
            renderer = new EquirectangularFrameRenderer(options, ignoreCamDir.isChecked());
        }
        options.setRenderer(renderer);

        ReplayHandler.startPath(options);
    }

    private int getWidthSetting() {
        return customResolution.isChecked() ? xRes.getIntValue() : mc.displayWidth;
    }

    private int getHeightSetting() {
        return customResolution.isChecked() ? yRes.getIntValue() : mc.displayHeight;
    }

    private class RendererDropdownListener implements SelectionListener {
        @Override
        public void onSelectionChanged(int selectionIndex) {
            RendererSettings s = rendererDropdown.getElement(selectionIndex);

            if(s == RendererSettings.DEFAULT) {
                youtubeExport.enabled = ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.STEREOSCOPIC) {
                youtubeExport.enabled = ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.CUBIC) {
                youtubeExport.enabled = true;
                ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.EQUIRECTANGULAR) {
                youtubeExport.enabled = ignoreCamDir.enabled = true;
            }


            xRes.setCursorPositionEnd();
            xRes.setSelectionPos(xRes.getCursorPosition());

            yRes.setCursorPositionEnd();
            yRes.setSelectionPos(yRes.getCursorPosition());
        }
    }
}
