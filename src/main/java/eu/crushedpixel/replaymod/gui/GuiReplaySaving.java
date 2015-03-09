package eu.crushedpixel.replaymod.gui;

import java.awt.Color;

import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.FMLClientHandler;

public class GuiReplaySaving extends GuiScreen {
	
	public static boolean replaySaving = false;
	
	private GuiScreen waiting = null;
	
	public GuiReplaySaving(GuiScreen waiting) {
		this.waiting = waiting;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Saving Replay File...", this.width / 2, 20, 16777215);
		this.drawCenteredString(this.fontRendererObj, "Please wait while your recent Replay is being saved.", this.width / 2, 40, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
		if(!replaySaving) {
			Minecraft.getMinecraft().displayGuiScreen(waiting);
		}
	}
	
}
