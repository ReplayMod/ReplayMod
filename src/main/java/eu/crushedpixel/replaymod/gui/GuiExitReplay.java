package eu.crushedpixel.replaymod.gui;

import java.io.IOException;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings.Options;

public class GuiExitReplay extends GuiIngameMenu {

	@Override
	public void initGui()
	{
		this.buttonList.clear();
		byte b0 = -16;
		boolean flag = true;
		this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120 + b0, I18n.format("menu.returnToMenu", new Object[0])));

		if (!this.mc.isIntegratedServerRunning())
		{
			((GuiButton)this.buttonList.get(0)).displayString = I18n.format("Exit Replay", new Object[0]);
		}

		this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 24 + b0, I18n.format("menu.returnToGame", new Object[0])));
		this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96 + b0, 98, 20, I18n.format("menu.options", new Object[0])));
		this.buttonList.add(new GuiButton(12, this.width / 2 + 2, this.height / 4 + 96 + b0, 98, 20, I18n.format("fml.menu.modoptions")));
		GuiButton guibutton;
		this.buttonList.add(guibutton = new GuiButton(7, this.width / 2 - 100, this.height / 4 + 72 + b0, 200, 20, I18n.format("menu.shareToLan", new Object[0])));
		this.buttonList.add(new GuiButton(5, this.width / 2 - 100, this.height / 4 + 48 + b0, 98, 20, I18n.format("gui.achievements", new Object[0])));
		this.buttonList.add(new GuiButton(6, this.width / 2 + 2, this.height / 4 + 48 + b0, 98, 20, I18n.format("gui.stats", new Object[0])));
		guibutton.enabled = this.mc.isSingleplayer() && !this.mc.getIntegratedServer().getPublic();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException 
	{
		if(button.id == 1) {
			button.enabled = false;

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					mc.displayGuiScreen(new GuiReplaySaving());
					ReplayHandler.endReplay();
					ReplayHandler.setSpeed(1f);

					mc.gameSettings.setOptionFloatValue(Options.GAMMA, ReplayHandler.getInitialGamma());
					
					ReplayHandler.lastExit = System.currentTimeMillis();
					mc.theWorld.sendQuittingDisconnectingPacket();
				}
			});
			
			t.run();

		} else {
			super.actionPerformed(button);
		}
	}
}
