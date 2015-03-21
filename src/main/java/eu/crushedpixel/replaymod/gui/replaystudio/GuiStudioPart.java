package eu.crushedpixel.replaymod.gui.replaystudio;

import java.io.IOException;

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
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
