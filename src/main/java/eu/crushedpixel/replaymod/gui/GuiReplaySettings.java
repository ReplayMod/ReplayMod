package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.gui.elements.GuiSettingsOnOffButton;
import eu.crushedpixel.replaymod.gui.elements.GuiToggleButton;
import eu.crushedpixel.replaymod.settings.ReplaySettings.RecordingOptions;
import eu.crushedpixel.replaymod.settings.ReplaySettings.ReplayOptions;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.awt.*;
import java.io.IOException;

import static eu.crushedpixel.replaymod.gui.GuiConstants.*;

public class GuiReplaySettings extends GuiScreen {
    protected String screenTitle = I18n.format("replaymod.gui.settings.title");
    private GuiScreen parentGuiScreen;

    public GuiReplaySettings(GuiScreen parentGuiScreen) {
        this.parentGuiScreen = parentGuiScreen;
    }

    @Override
    public void initGui() {
        this.screenTitle = I18n.format("replaymod.gui.settings.title");

        @SuppressWarnings("unchecked")
        java.util.List<GuiButton> buttonList = this.buttonList;

        buttonList.clear();
        buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done")));

        int i = 0;

        for(RecordingOptions o : RecordingOptions.values()) {

            int xPos = this.width / 2 - 155 + i % 2 * 160;
            int yPos = this.height / 6 + 24 * (i >> 1);

            if(o == RecordingOptions.notifications) {
                GuiToggleButton sendChatButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_SEND_CHAT, xPos, yPos, 150, 20, o);
                buttonList.add(sendChatButton);

            } else if(o == RecordingOptions.recordServer) {
                GuiToggleButton recordServerButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_RECORDSERVER_ID, xPos, yPos, 150, 20, o);
                buttonList.add(recordServerButton);

            } else if(o == RecordingOptions.recordSingleplayer) {
                GuiToggleButton recordSPButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_RECORDSP_ID, xPos, yPos, 150, 20, o);
                buttonList.add(recordSPButton);

            } else if(o == RecordingOptions.indicator) {
                GuiToggleButton showIndicatorButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_INDICATOR_ID, xPos, yPos, 150, 20, o);
                buttonList.add(showIndicatorButton);
            }

            ++i;
        }


        if(i % 2 == 1) {
            ++i;
        }

        for(ReplayOptions o : ReplayOptions.values()) {

            int xPos = this.width / 2 - 155 + i % 2 * 160;
            int yPos = this.height / 6 + 24 * (i >> 1);

            if(o == ReplayOptions.lighting) {
                GuiToggleButton lightingButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_ENABLE_LIGHTING, xPos, yPos, 150, 20, o);
                buttonList.add(lightingButton);

            } else if(o == ReplayOptions.linear) {
                GuiToggleButton linearButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_FORCE_LINEAR, xPos, yPos, 150, 20, o,
                        I18n.format("replaymod.gui.settings.interpolation.linear"), I18n.format("replaymod.gui.settings.interpolation.cubic"));
                buttonList.add(linearButton);

            } else if(o == ReplayOptions.previewPath) {
                GuiToggleButton pathPreviewButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_PATHPREVIEW_ID, xPos, yPos, 150, 20, o);
                buttonList.add(pathPreviewButton);

            } else if(o == ReplayOptions.keyframeCleanCallback) {
                GuiToggleButton keyframeClearCallbackButton = new GuiSettingsOnOffButton(REPLAY_SETTINGS_CLEARCALLBACK_ID, xPos, yPos, 150, 20, o);
                buttonList.add(keyframeClearCallbackButton);
            } else if(o == ReplayOptions.showChat) {
                buttonList.add(new GuiSettingsOnOffButton(0, xPos, yPos, 150, 20, o));
            }

            ++i;
        }

        if(i % 2 == 1) {
            ++i;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, screenTitle, this.width / 2, 20, 16777215);
        if(FMLClientHandler.instance().getClient().thePlayer != null) {
            this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.settings.warning.linea"), this.width / 2, 180, Color.RED.getRGB());
            this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.settings.warning.lineb"), this.width / 2, 190, Color.RED.getRGB());
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled) {
            switch(button.id) {
                case 200:
                    this.mc.displayGuiScreen(this.parentGuiScreen);
                    break;
            }
        }
    }
}
