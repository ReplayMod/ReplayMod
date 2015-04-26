package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.settings.ReplaySettings.RecordingOptions;
import eu.crushedpixel.replaymod.settings.ReplaySettings.RenderOptions;
import eu.crushedpixel.replaymod.settings.ReplaySettings.ReplayOptions;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.awt.*;
import java.io.IOException;

public class GuiReplaySettings extends GuiScreen {

    //TODO: Move to GuiConstants
    private static final int QUALITY_SLIDER_ID = 9003;
    private static final int RECORDSERVER_ID = 9004;
    private static final int RECORDSP_ID = 9005;
    private static final int SEND_CHAT = 9006;
    private static final int FORCE_LINEAR = 9007;
    private static final int ENABLE_LIGHTING = 9008;
    private static final int FRAMERATE_SLIDER_ID = 9009;
    private static final int RESOURCEPACK_ID = 9010;
    private static final int WAITFORCHUNKS_ID = 9011;
    private static final int INDICATOR_ID = 9012;
    protected String screenTitle = I18n.format("replaymod.gui.settings.title");
    private GuiScreen parentGuiScreen;
    private GuiButton recordServerButton, recordSPButton, sendChatButton, linearButton, lightingButton,
            resourcePackButton, waitForChunksButton, showIndicatorButton;

    public GuiReplaySettings(GuiScreen parentGuiScreen) {
        this.parentGuiScreen = parentGuiScreen;
    }

    public void initGui() {
        this.screenTitle = I18n.format("replaymod.gui.settings.title");
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done")));

        ReplaySettings settings = ReplayMod.replaySettings;

        int k = 0;
        int i = 0;

        for(RecordingOptions o : RecordingOptions.values()) {
            if(o == RecordingOptions.notifications) {
                this.buttonList.add(sendChatButton = new GuiButton(SEND_CHAT,
                        this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20,
                        I18n.format("replaymod.gui.settings.notifications")+": " + onOff(settings.isShowNotifications())));
            } else if(o == RecordingOptions.recordServer) {
                this.buttonList.add(recordServerButton = new GuiButton(RECORDSERVER_ID,
                        this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20,
                        I18n.format("replaymod.gui.settings.recordserver")+": "
                        + onOff(settings.isEnableRecordingServer())));
            } else if(o == RecordingOptions.recordSingleplayer) {
                this.buttonList.add(recordSPButton = new GuiButton(RECORDSP_ID, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.recordsingleplayer")+": "
                        + onOff(settings.isEnableRecordingSingleplayer())));
            } else if(o == RecordingOptions.indicator) {
                this.buttonList.add(showIndicatorButton = new GuiButton(INDICATOR_ID, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.indicator")+": "+ onOff(settings.showRecordingIndicator())));
            }

            ++i;
            ++k;
        }


        if(i % 2 == 1) {
            ++i;
        }

        for(ReplayOptions o : ReplayOptions.values()) {
            if(o == ReplayOptions.lighting) {
                this.buttonList.add(lightingButton = new GuiButton(ENABLE_LIGHTING, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.lighting")+": " + onOff(settings.isLightingEnabled())));
            } else if(o == ReplayOptions.linear) {
                this.buttonList.add(linearButton = new GuiButton(FORCE_LINEAR, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.interpolation")+": " + linearOnOff(settings.isLinearMovement())));
            } else if(o == ReplayOptions.useResources) {
                this.buttonList.add(resourcePackButton = new GuiButton(RESOURCEPACK_ID, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.resources")+": " + onOff(settings.getUseResourcePacks())));
            }

            ++i;
            ++k;
        }

        if(i % 2 == 1) {
            ++i;
        }

        for(RenderOptions o : RenderOptions.values()) {
            if(o == RenderOptions.videoFramerate) {
                this.buttonList.add(new GuiVideoFramerateSlider(FRAMERATE_SLIDER_ID,
                        this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), settings.getVideoFramerate(), "Video Framerate"));
            } else if(o == RenderOptions.videoQuality) {
                this.buttonList.add(new GuiVideoQualitySlider(QUALITY_SLIDER_ID,
                        this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), (float) settings.getVideoQuality(), "Video Quality"));
            } else if(o == RenderOptions.waitForChunks) {
                this.buttonList.add(resourcePackButton = new GuiButton(WAITFORCHUNKS_ID, this.width / 2 - 155 + i % 2 * 160,
                        this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("replaymod.gui.settings.forcechunks")+": " + onOff(settings.getWaitForChunks())));
            }

            ++i;
            ++k;
        }
    }

    private String onOff(boolean on) {
        return on ? I18n.format("options.on") : I18n.format("options.off");
    }

    private String linearOnOff(boolean on) {
        return on ? I18n.format("replaymod.gui.settings.interpolation.linear") : I18n.format("replaymod.gui.settings.interpolation.cubic");
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
                case RECORDSERVER_ID:
                    boolean enabled = ReplayMod.replaySettings.isEnableRecordingServer();
                    enabled = !enabled;
                    recordServerButton.displayString = I18n.format("replaymod.gui.settings.recordserver")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setEnableRecordingServer(enabled);
                    break;
                case RECORDSP_ID:
                    enabled = ReplayMod.replaySettings.isEnableRecordingSingleplayer();
                    enabled = !enabled;
                    recordSPButton.displayString = I18n.format("replaymod.gui.settings.singleplayer")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setEnableRecordingSingleplayer(enabled);
                    break;
                case SEND_CHAT:
                    enabled = ReplayMod.replaySettings.isShowNotifications();
                    enabled = !enabled;
                    sendChatButton.displayString = I18n.format("replaymod.gui.settings.notifications")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setShowNotifications(enabled);
                    break;
                case FORCE_LINEAR:
                    enabled = ReplayMod.replaySettings.isLinearMovement();
                    enabled = !enabled;
                    linearButton.displayString = I18n.format("replaymod.gui.settings.interpolation")+": " + linearOnOff(enabled);
                    ReplayMod.replaySettings.setLinearMovement(enabled);
                    break;
                case ENABLE_LIGHTING:
                    enabled = ReplayMod.replaySettings.isLightingEnabled();
                    enabled = !enabled;
                    lightingButton.displayString = I18n.format("replaymod.gui.settings.lighting")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setLightingEnabled(enabled);
                    break;
                case RESOURCEPACK_ID:
                    enabled = ReplayMod.replaySettings.getUseResourcePacks();
                    enabled = !enabled;
                    resourcePackButton.displayString = I18n.format("replaymod.gui.settings.resources")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setUseResourcePacks(enabled);
                    break;
                case WAITFORCHUNKS_ID:
                    enabled = ReplayMod.replaySettings.getWaitForChunks();
                    enabled = !enabled;
                    resourcePackButton.displayString = I18n.format("replaymod.gui.settings.forcechunks")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setWaitForChunks(enabled);
                    break;
                case INDICATOR_ID:
                    enabled = ReplayMod.replaySettings.showRecordingIndicator();
                    enabled = !enabled;
                    showIndicatorButton.displayString = I18n.format("replaymod.gui.settings.indicator")+": " + onOff(enabled);
                    ReplayMod.replaySettings.setEnableIndicator(enabled);
                    break;
            }
        }
    }
}
