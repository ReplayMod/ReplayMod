package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiNumberInput extends GuiTextField {

	private int limit;
	
	public GuiNumberInput(int id, FontRenderer fontRenderer,
			int xPos, int yPos, int width, int limit) {
		super(id, fontRenderer, xPos, yPos, width, 20);
		this.limit = limit;
	}
	
	@Override
	public void writeText(String text) {
		try {
			Integer.valueOf(text);
			if(limit > 0 && (getText()+text).length() > limit) return;
			super.writeText(text);
		} catch(NumberFormatException e) {}
	}

}
