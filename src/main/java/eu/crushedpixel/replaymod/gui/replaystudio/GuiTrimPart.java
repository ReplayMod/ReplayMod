package eu.crushedpixel.replaymod.gui.replaystudio;

import java.awt.Color;

import net.minecraft.client.gui.GuiScreen;

public class GuiTrimPart extends GuiScreen implements GuiStudioPart{

	private int yPos = 0;
	
	public GuiTrimPart(int yPos) {
		this.yPos = yPos;
	}
	
	@Override
	public void applyFilters() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void initGui() {
		super.initGui();
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawString(fontRendererObj, "Start", 30, 10+yPos, Color.WHITE.getRGB());
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
}
