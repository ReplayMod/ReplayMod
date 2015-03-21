package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import eu.crushedpixel.replaymod.ReplayMod;

public class GuiVideoFramerateSlider extends GuiButton {

	public GuiVideoFramerateSlider(int buttonId, int p_i45017_2_, int p_i45017_3_, int initialFramerate, String displayKey) {
		super(buttonId, p_i45017_2_, p_i45017_3_, 150, 20, "");
		this.sliderValue = normalizeValue(initialFramerate);
		this.displayString = displayKey+": "+translate(initialFramerate);
		this.displayKey = displayKey;
	}
	
	private String displayKey;
	private float sliderValue;
	public boolean dragging;
	
	private String translate(int value) {
		return String.valueOf(value);
	}

	protected int getHoverState(boolean mouseOver) {
		return 0;
	}

	@Override
	protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
		if (this.visible) {
			if (this.dragging) {
				sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
				sliderValue = MathHelper.clamp_float(sliderValue, 0.0F, 1.0F);
				int f = denormalizeValue(sliderValue);
				this.displayString = displayKey+": "+translate(f);
				ReplayMod.replaySettings.setVideoFramerate(f);
			}

			mc.getTextureManager().bindTexture(buttonTextures);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
		}
	}

	private float normalizeValue(int val) {
		 return (val-10)/110f;
	}
	
	private int denormalizeValue(float val) {
		//Transfers the value ranging from 0.0 to 1.0 to the scale of 10 to 120
		float r = 110f*val;
		return Math.round(10+r);
	}

	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (super.mousePressed(mc, mouseX, mouseY)) {
			this.dragging = true;
			return true;
		} else {
			return false;
		}
	}

	public void mouseReleased(int mouseX, int mouseY) {
		this.dragging = false;
	}
}
