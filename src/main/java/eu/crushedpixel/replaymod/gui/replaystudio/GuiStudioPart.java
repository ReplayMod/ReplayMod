package eu.crushedpixel.replaymod.gui.replaystudio;

import net.minecraft.client.gui.GuiScreen;

public abstract class GuiStudioPart extends GuiScreen {
	
	public GuiStudioPart(int yPos) {
		this.yPos = yPos;
	}
	
	protected int yPos = 0;
	
	public abstract void applyFilters();
	
	public abstract String getDescription();
	
	public abstract String getTitle();
	
	@Override
	public abstract void keyTyped(char typedChar, int keyCode);
	
	@Override
	public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);
}
