package eu.crushedpixel.replaymod.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.settings.ReplaySettings;

public class GuiReplaySettings extends GuiScreen {

	private GuiScreen parentGuiScreen;
	protected String screenTitle = "Replay Mod Settings";

	private static final int MAXSIZE_SLIDER_ID = 9003;
	private static final int RECORDSERVER_ID = 9004;
	private static final int RECORDSP_ID = 9005;
	private static final int SEND_CHAT = 9006;
	private static final int FORCE_LINEAR = 9007;

	private GuiButton recordServerButton, recordSPButton, sendChatButton, linearButton;

	public GuiReplaySettings(GuiScreen parentGuiScreen)
	{
		this.parentGuiScreen = parentGuiScreen;
	}

	public void initGui() {
		this.screenTitle = I18n.format("Replay Mod Settings", new Object[0]);
		this.buttonList.clear();
		this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done", new Object[0])));

		Map<String, Object> aoptions = ReplayMod.replaySettings.getOptions();

		int k = 0;
		int i = 0;
		for (Entry<String, Object> e : aoptions.entrySet()) {
			if(e.getKey().equals("Maximum File Size")) {
				float minValue = -1;
				float maxValue = 10000;
				float valueSteps = 1;

				int val = (Integer)e.getValue();
				this.buttonList.add(new GuiSizeLimitOptionSlider(MAXSIZE_SLIDER_ID, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 0, 39, 1, val, e.getKey()));

			} else if(e.getKey().equals("Enable Notifications")) {
				sendChatButton = new GuiButton(SEND_CHAT, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, "Enable Notifications: "+onOff((Boolean)e.getValue()));
				this.buttonList.add(sendChatButton);
			} else if(e.getKey().equals("Record Server")) {
				recordServerButton = new GuiButton(RECORDSERVER_ID, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, "Record Server: "+onOff((Boolean)e.getValue()));
				this.buttonList.add(recordServerButton);
			} else if(e.getKey().equals("Record Singleplayer")) {
				recordSPButton = new GuiButton(RECORDSP_ID, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, "Record Singleplayer: "+onOff((Boolean)e.getValue()));
				this.buttonList.add(recordSPButton);
			} else if(e.getKey().equals("Force Linear Movement")) {
				linearButton = new GuiButton(FORCE_LINEAR, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, "Camera Path: "+linearOnOff((Boolean)e.getValue()));
				this.buttonList.add(linearButton);
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
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Replay Mod Settings", this.width / 2, 20, 16777215);
		if (FMLClientHandler.instance().getClient().thePlayer != null) {
			this.drawCenteredString(this.fontRendererObj, "WARNING: These settings are going to be", this.width / 2, 180, Color.RED.getRGB());
			this.drawCenteredString(this.fontRendererObj, "applied the next time you join a world.", this.width / 2, 190, Color.RED.getRGB());
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}


	protected void actionPerformed(GuiButton button) throws IOException
	{
		if (button.enabled)
		{
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
			}
		}
	}
}
