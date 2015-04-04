package eu.crushedpixel.replaymod.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.Map.Entry;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S14PacketEntity.S15PacketEntityRelMove;
import net.minecraft.network.play.server.S14PacketEntity.S16PacketEntityLook;
import net.minecraftforge.fml.client.FMLClientHandler;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.settings.ReplaySettings.Option;

public class GuiReplaySettings extends GuiScreen {

	private GuiScreen parentGuiScreen;
	protected String screenTitle = "Replay Mod Settings";

	//TODO: Move to GuiConstants
	private static final int QUALITY_SLIDER_ID = 9003;
	private static final int RECORDSERVER_ID = 9004;
	private static final int RECORDSP_ID = 9005;
	private static final int SEND_CHAT = 9006;
	private static final int FORCE_LINEAR = 9007;
	private static final int ENABLE_LIGHTING = 9008;
	private static final int FRAMERATE_SLIDER_ID = 9009;
	private static final int RESOURCEPACK_ID = 9010;

	private GuiButton recordServerButton, recordSPButton, sendChatButton, linearButton, lightingButton, resourcePackButton;

	public GuiReplaySettings(GuiScreen parentGuiScreen) {
		this.parentGuiScreen = parentGuiScreen;
	}

	public void initGui() {
		this.screenTitle = I18n.format("Replay Mod Settings", new Object[0]);
		this.buttonList.clear();
		this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done", new Object[0])));

		Option[] aoptions = Option.values();

		ReplaySettings settings = ReplayMod.replaySettings;
		
		int k = 0;
		int i = 0;
		for (Option o : aoptions) {

			switch(o) {
			case lighting:
				this.buttonList.add(lightingButton = new GuiButton(ENABLE_LIGHTING, this.width / 2 - 155 + i % 2 * 160, 
						this.height / 6 + 24 * (i >> 1), 150, 20, "Enable Lighting: "+onOff(settings.isLightingEnabled())));
				break;
			case linear:
				this.buttonList.add(linearButton = new GuiButton(FORCE_LINEAR, this.width / 2 - 155 + i % 2 * 160, 
						this.height / 6 + 24 * (i >> 1), 150, 20, "Camera Path: "+linearOnOff(settings.isLinearMovement())));
				break;
			case notifications:
				this.buttonList.add(sendChatButton = new GuiButton(SEND_CHAT, 
						this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, 
						"Enable Notifications: "+onOff(settings.isShowNotifications())));
				break;
			case recordServer:
				this.buttonList.add(recordServerButton = new GuiButton(RECORDSERVER_ID, 
						this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, "Record Server: "
								+onOff(settings.isEnableRecordingServer())));
				break;
			case recordSingleplayer:
				this.buttonList.add(recordSPButton = new GuiButton(RECORDSP_ID, this.width / 2 - 155 + i % 2 * 160, 
						this.height / 6 + 24 * (i >> 1), 150, 20, "Record Singleplayer: "+onOff(settings.isEnableRecordingSingleplayer())));
				break;
			case useResources:
				this.buttonList.add(resourcePackButton = new GuiButton(RESOURCEPACK_ID, this.width / 2 - 155 + i % 2 * 160, 
						this.height / 6 + 24 * (i >> 1), 150, 20, "Server Resource Packs: "+onOff(settings.getUseResourcePacks())));
				break;
			case videoFramerate:
				this.buttonList.add(new GuiVideoFramerateSlider(FRAMERATE_SLIDER_ID, 
						this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), settings.getVideoFramerate(), "Video Framerate"));
				break;
			case videoQuality:
				this.buttonList.add(new GuiVideoQualitySlider(QUALITY_SLIDER_ID, 
						this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), (float)settings.getVideoQuality(), "Video Quality"));
				break;
			default:
				break;
			}

			++i;
			++k;
		}

		if (i % 2 == 1)
		{
			++i;
		}
	}

	private String onOff(boolean on) {
		return on ? "ON" : "OFF";
	}

	private String linearOnOff(boolean on) {
		return on ? "Linear" : "Cubic";
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Replay Mod Settings", this.width / 2, 20, 16777215);
		if (FMLClientHandler.instance().getClient().thePlayer != null) {
			this.drawCenteredString(this.fontRendererObj, "WARNING: Recording settings are going to be", this.width / 2, 180, Color.RED.getRGB());
			this.drawCenteredString(this.fontRendererObj, "applied the next time you join a world.", this.width / 2, 190, Color.RED.getRGB());
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}


	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.enabled) {
			switch(button.id) {
			case 200:
				this.mc.displayGuiScreen(this.parentGuiScreen);
				break;
			case RECORDSERVER_ID:
				boolean enabled = ReplayMod.replaySettings.isEnableRecordingServer();
				enabled = !enabled;
				recordServerButton.displayString = "Record Server: "+onOff(enabled);
				ReplayMod.replaySettings.setEnableRecordingServer(enabled);
				break;
			case RECORDSP_ID:
				enabled = ReplayMod.replaySettings.isEnableRecordingSingleplayer();
				enabled = !enabled;
				recordSPButton.displayString = "Record Singleplayer: "+onOff(enabled);
				ReplayMod.replaySettings.setEnableRecordingSingleplayer(enabled);
				break;
			case SEND_CHAT:
				enabled = ReplayMod.replaySettings.isShowNotifications();
				enabled = !enabled;
				sendChatButton.displayString = "Enable Notifications: "+onOff(enabled);
				ReplayMod.replaySettings.setShowNotifications(enabled);
				break;
			case FORCE_LINEAR:
				enabled = ReplayMod.replaySettings.isLinearMovement();
				enabled = !enabled;
				linearButton.displayString = "Camera Path: "+linearOnOff(enabled);
				ReplayMod.replaySettings.setLinearMovement(enabled);
				break;
			case ENABLE_LIGHTING:
				enabled = ReplayMod.replaySettings.isLightingEnabled();
				enabled = !enabled;
				lightingButton.displayString = "Enable Lighting: "+onOff(enabled);
				ReplayMod.replaySettings.setLightingEnabled(enabled);
				break;
			case RESOURCEPACK_ID:
				enabled = ReplayMod.replaySettings.getUseResourcePacks();
				enabled = !enabled;
				resourcePackButton.displayString = "Server Resource Packs: "+onOff(enabled);
				ReplayMod.replaySettings.setUseResourcePacks(enabled);
				break;
			}
		}
	}
}
