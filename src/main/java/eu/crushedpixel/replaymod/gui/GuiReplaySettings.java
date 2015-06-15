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

public class GuiReplaySettings extends GuiScreen {

    //TODO: Move to GuiConstants
    private static final int RECORDSERVER_ID = 9004;
    private static final int RECORDSP_ID = 9005;
    private static final int SEND_CHAT = 9006;
    private static final int FORCE_LINEAR = 9007;
    private static final int ENABLE_LIGHTING = 9008;
    private static final int RESOURCEPACK_ID = 9010;
    private static final int INDICATOR_ID = 9012;
    private static final int PATHPREVIEW_ID = 9013;
    protected String screenTitle = I18n.format("replaymod.gui.settings.title");
    private GuiScreen parentGuiScreen;
    private GuiToggleButton recordServerButton, recordSPButton, sendChatButton, linearButton, lightingButton,
            resourcePackButton, showIndicatorButton, pathPreviewButton;

    public GuiReplaySettings(GuiScreen parentGuiScreen) {
        this.parentGuiScreen = parentGuiScreen;
    }

    public void initGui() {
        this.screenTitle = I18n.format("replaymod.gui.settings.title");
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done")));

        int i = 0;

        for(RecordingOptions o : RecordingOptions.values()) {

            int xPos = this.width / 2 - 155 + i % 2 * 160;
            int yPos = this.height / 6 + 24 * (i >> 1);

            if(o == RecordingOptions.notifications) {
                sendChatButton = new GuiSettingsOnOffButton(SEND_CHAT, xPos, yPos, 150, 20, o);
                this.buttonList.add(sendChatButton);

            } else if(o == RecordingOptions.recordServer) {
                recordServerButton = new GuiSettingsOnOffButton(RECORDSERVER_ID, xPos, yPos, 150, 20, o);
                this.buttonList.add(recordServerButton);

            } else if(o == RecordingOptions.recordSingleplayer) {
                recordSPButton = new GuiSettingsOnOffButton(RECORDSP_ID, xPos, yPos, 150, 20, o);
                this.buttonList.add(recordSPButton);

            } else if(o == RecordingOptions.indicator) {
                showIndicatorButton = new GuiSettingsOnOffButton(INDICATOR_ID, xPos, yPos, 150, 20, o);
                this.buttonList.add(showIndicatorButton);
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
                lightingButton = new GuiSettingsOnOffButton(ENABLE_LIGHTING, xPos, yPos, 150, 20, o);
                this.buttonList.add(lightingButton);

            } else if(o == ReplayOptions.linear) {
                linearButton = new GuiSettingsOnOffButton(FORCE_LINEAR, xPos, yPos, 150, 20, o,
                        I18n.format("replaymod.gui.settings.interpolation.linear"), I18n.format("replaymod.gui.settings.interpolation.cubic"));
                this.buttonList.add(linearButton);

            } else if(o == ReplayOptions.useResources) {
                resourcePackButton = new GuiSettingsOnOffButton(RESOURCEPACK_ID, xPos, yPos, 150, 20, o);
                this.buttonList.add(resourcePackButton);

            } else if(o == ReplayOptions.previewPath) {
                pathPreviewButton = new GuiSettingsOnOffButton(PATHPREVIEW_ID, xPos, yPos, 150, 20, o);
                this.buttonList.add(pathPreviewButton);
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


    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled) {
            switch(button.id) {
                case 200:
                    this.mc.displayGuiScreen(this.parentGuiScreen);
                    break;
            }

            if(button instanceof GuiToggleButton) {
                ((GuiToggleButton) button).toggle();
            }
        }
    }
}
