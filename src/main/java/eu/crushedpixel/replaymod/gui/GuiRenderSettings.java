package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
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

public class GuiRenderSettings extends GuiScreen {

    private GuiButton renderButton, cancelButton;
    private GuiDropdown<RendererSettings> rendererDropdown;

    private int virtualY, virtualHeight;
    private int leftBorder = 10;

    private GuiCheckBox customResolution, ignoreCamDir, youtubeExport;
    private GuiNumberInput xRes, yRes;
    private GuiButton interpolation, forceChunks;
    private GuiVideoFramerateSlider framerateSlider;
    private GuiVideoQualitySlider qualitySlider;

    private int w1, w2, w3;

    private boolean initialized;

    private boolean linear = false;
    private boolean waitForChunks = true;

    private final Minecraft mc = Minecraft.getMinecraft();

    public GuiRenderSettings() {
        //if not enough keyframes, abort and leave chat message
        if(ReplayHandler.getPosKeyframeCount() < 2 && ReplayHandler.getTimeKeyframeCount() < 2) {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.notenoughkeyframes", ChatMessageHandler.ChatMessageType.WARNING);
            mc.displayGuiScreen(null);
            return;
        }
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        if(!initialized) {
            rendererDropdown = new GuiDropdown<RendererSettings>(GuiConstants.RENDER_SETTINGS_RENDERER_DROPDOWN,
                    fontRendererObj, 0, 0, 200, 5);
            rendererDropdown.addSelectionListener(new RendererDropdownListener());

            renderButton = new GuiButton(GuiConstants.RENDER_SETTINGS_RENDER_BUTTON, 0, 0, I18n.format("replaymod.gui.render"));
            cancelButton = new GuiButton(GuiConstants.RENDER_SETTINGS_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.cancel"));

            customResolution = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_RESOLUTION_CHECKBOX, 0, 0, I18n.format("replaymod.gui.rendersettings.customresolution"), false);
            customResolution.enabled = false;

            xRes = new GuiNumberInput(GuiConstants.RENDER_SETTINGS_RESOLUTION_X, fontRendererObj, 0, 0, 50, 1, 100000, mc.displayWidth, false);
            yRes = new GuiNumberInput(GuiConstants.RENDER_SETTINGS_RESOLUTION_Y, fontRendererObj, 0, 0, 50, 1, 100000, mc.displayHeight, false);

            xRes.setEnabled(false);
            yRes.setEnabled(false);

            interpolation = new GuiButton(GuiConstants.RENDER_SETTINGS_INTERPOLATION_BUTTON, 0, 0, getInterpolationDisplayString());
            forceChunks = new GuiButton(GuiConstants.RENDER_SETTINGS_FORCECHUNKS_BUTTON, 0, 0, getForceChunksDisplayString());

            framerateSlider = new GuiVideoFramerateSlider(GuiConstants.RENDER_SETTINGS_FRAMERATE_SLIDER, 0, 0, ReplayMod.replaySettings.getVideoFramerate(),
                    I18n.format("replaymod.gui.rendersettings.framerate"));
            qualitySlider = new GuiVideoQualitySlider(GuiConstants.RENDER_SETTINGS_QUALITY_SLIDER, 0, 0, (float)ReplayMod.replaySettings.getVideoQuality(),
                    I18n.format("replaymod.gui.rendersettings.quality"));

            forceChunks.width = interpolation.width = framerateSlider.width = qualitySlider.width = 150;

            ignoreCamDir = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_STATIC_CAMERA, 0, 0, I18n.format("replaymod.gui.rendersettings.stablecamera"), false);
            youtubeExport = new GuiCheckBox(GuiConstants.RENDER_SETTINGS_YOUTUBE_READY, 0, 0, I18n.format("replaymod.gui.rendersettings.exportyoutube"), true);
            ignoreCamDir.enabled = youtubeExport.enabled = false;

            for(RendererSettings r : RendererSettings.values()) {
                rendererDropdown.addElement(r);
            }
        }

        virtualHeight = 200;
        virtualY = (this.height-virtualHeight)/2;

        cancelButton.width = renderButton.width = 100;

        cancelButton.xPosition = width-10-5-100;
        renderButton.xPosition = cancelButton.xPosition-5-100;

        cancelButton.yPosition = renderButton.yPosition = virtualY+virtualHeight-5-18;

        buttonList.add(cancelButton);
        buttonList.add(renderButton);

        w1 = rendererDropdown.width + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.rendersettings.renderer") + ":")+10;
        rendererDropdown.yPosition = virtualY + 15 + 15;
        rendererDropdown.xPosition = (width-w1)/2 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.rendersettings.renderer") + ":")+10;

        w2 = customResolution.width+5+xRes.width+5+fontRendererObj.getStringWidth("*")+5+yRes.width;

        customResolution.yPosition = virtualY + 15 + 5 + 20 + 10 +5+fontRendererObj.getStringWidth("*")+5;
        customResolution.xPosition = (width-w2)/2;
        buttonList.add(customResolution);

        xRes.xPosition = customResolution.xPosition + customResolution.width + 5;
        yRes.xPosition = xRes.xPosition+xRes.width+5+fontRendererObj.getStringWidth("*")+5;
        xRes.yPosition = yRes.yPosition = customResolution.yPosition-3;

        w3 = interpolation.width + 10 + forceChunks.width;

        interpolation.xPosition = (width-w3)/2;
        interpolation.yPosition = xRes.yPosition+20+10;
        buttonList.add(interpolation);

        forceChunks.xPosition = interpolation.xPosition+interpolation.width+10;
        forceChunks.yPosition = interpolation.yPosition;
        buttonList.add(forceChunks);

        framerateSlider.xPosition = interpolation.xPosition;
        qualitySlider.xPosition = forceChunks.xPosition;
        framerateSlider.yPosition = qualitySlider.yPosition = interpolation.yPosition + 20 + 10;

        buttonList.add(framerateSlider);
        buttonList.add(qualitySlider);

        ignoreCamDir.xPosition = framerateSlider.xPosition + (framerateSlider.width - ignoreCamDir.width)/2;
        youtubeExport.xPosition = qualitySlider.xPosition + (qualitySlider.width - youtubeExport.width)/2;
        
        ignoreCamDir.yPosition = youtubeExport.yPosition = framerateSlider.yPosition+20+10;

        buttonList.add(ignoreCamDir);
        buttonList.add(youtubeExport);

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(leftBorder, virtualY, width - leftBorder, virtualY + virtualHeight, -1072689136, -804253680);

        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.rendersettings.title"),
                this.width / 2, virtualY + 5, Color.WHITE.getRGB());

        this.drawString(fontRendererObj, I18n.format("replaymod.gui.rendersettings.renderer") + ":",
                (width-w1)/2, rendererDropdown.yPosition + 8, Color.WHITE.getRGB());

        xRes.drawTextBox();
        yRes.drawTextBox();
        this.drawString(fontRendererObj, "*", xRes.xPosition + xRes.width + 5, xRes.yPosition + 3, Color.WHITE.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);

        rendererDropdown.drawTextBox();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(!rendererDropdown.mouseClickedResult(mouseX, mouseY, mouseButton)) {
            xRes.mouseClicked(mouseX, mouseY, mouseButton);
            yRes.mouseClicked(mouseX, mouseY, mouseButton);
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
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
        if(button.id == GuiConstants.RENDER_SETTINGS_RENDER_BUTTON) {
            startRendering();
        } else if(button.id == GuiConstants.RENDER_SETTINGS_CANCEL_BUTTON) {
            mc.displayGuiScreen(null);
        } else if(button.id == GuiConstants.RENDER_SETTINGS_RESOLUTION_CHECKBOX) {
            boolean enabled = ((GuiCheckBox)button).isChecked();
            xRes.setEnabled(enabled);
            yRes.setEnabled(enabled);
        } else if(button.id == GuiConstants.RENDER_SETTINGS_INTERPOLATION_BUTTON) {
            linear = !linear;
            button.displayString = getInterpolationDisplayString();
        } else if(button.id == GuiConstants.RENDER_SETTINGS_FORCECHUNKS_BUTTON) {
            waitForChunks = !waitForChunks;
            button.displayString = getForceChunksDisplayString();
        }
    }

    private String getInterpolationDisplayString() {
        return I18n.format("replaymod.gui.rendersettings.interpolation")+": "+
                (linear ? I18n.format("replaymod.gui.settings.interpolation.linear") :
                        I18n.format("replaymod.gui.settings.interpolation.cubic"));
    }

    private String getForceChunksDisplayString() {
        return I18n.format("replaymod.gui.rendersettings.forcechunks")+": "+
                (waitForChunks ? I18n.format("options.on") : I18n.format("options.off"));
    }

    private enum RendererSettings {
        DEFAULT("default"),
        TILED("tiled"),
        STEREOSCOPIC("stereoscopic"),
        CUBIC("cubic"),
        EQUIRECTANGULAR("equirectangular");

        private String name, desc;

        RendererSettings(String name) {
            this.name = "replaymod.gui.rendersettings.renderer."+name;
            this.desc = name+".description";
        }

        @Override
        public String toString() {
            return I18n.format(name);
        }

        public String getDescription() {
            return desc;
        }
    }

    private void startRendering() {
        FrameRenderer renderer = null;

        RendererSettings r = rendererDropdown.getElement(rendererDropdown.getSelectionIndex());

        RenderOptions options = new RenderOptions();

        options.setLinearMovement(linear);
        ReplayMod.replaySettings.setLinearMovement(linear);

        options.setWaitForChunks(waitForChunks);
        ReplayMod.replaySettings.setWaitForChunks(waitForChunks);

        options.setFps(framerateSlider.getFPS());
        ReplayMod.replaySettings.setVideoFramerate(framerateSlider.getFPS());

        options.setQuality(qualitySlider.getQuality());
        ReplayMod.replaySettings.setVideoQuality(qualitySlider.getQuality());

        if(r == RendererSettings.DEFAULT) {
            renderer = new DefaultFrameRenderer(options);
        } else if(r == RendererSettings.TILED) {
            renderer = new TilingFrameRenderer(options, getWidthSetting(), getHeightSetting());
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
                customResolution.enabled = false;
                xRes.setEnabled(false);
                yRes.setEnabled(false);
                youtubeExport.enabled = ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.TILED) {
                customResolution.enabled = true;
                xRes.setEnabled(customResolution.isChecked());
                yRes.setEnabled(customResolution.isChecked());
                youtubeExport.enabled = ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.STEREOSCOPIC) {
                customResolution.enabled = false;
                xRes.setEnabled(false);
                yRes.setEnabled(false);
                youtubeExport.enabled = ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.CUBIC) {
                customResolution.enabled = false;
                xRes.setEnabled(false);
                yRes.setEnabled(false);
                youtubeExport.enabled = true;
                ignoreCamDir.enabled = false;
            } else if(s == RendererSettings.EQUIRECTANGULAR) {
                customResolution.enabled = false;
                xRes.setEnabled(false);
                yRes.setEnabled(false);
                youtubeExport.enabled = ignoreCamDir.enabled = true;
            }
        }
    }
}
