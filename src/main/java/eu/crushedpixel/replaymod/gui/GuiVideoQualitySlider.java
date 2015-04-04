package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import eu.crushedpixel.replaymod.ReplayMod;

public class GuiVideoQualitySlider extends GuiButton {

	public GuiVideoQualitySlider(int buttonId, int p_i45017_2_, int p_i45017_3_, float d, String displayKey) {
		super(buttonId, p_i45017_2_, p_i45017_3_, 150, 20, "");
		this.sliderValue = normalizeValue(d);
		this.displayString = displayKey+": "+translate(d);
		this.displayKey = displayKey;
	}
	
	private String displayKey;
	private float sliderValue;
	public boolean dragging;
	
	private String translate(float value) {
		if(value <= 0.3) {
			return "Draft";
		} else if(value <= 0.5) {
			return "Normal";
		} else if(value <= 0.7) {
			return "Good";
		}
		return "Best";
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
				float f = denormalizeValue(sliderValue);
				f = snapValue(f);
				sliderValue = normalizeValue(f);
				this.displayString = displayKey+": "+translate(f);
				ReplayMod.replaySettings.setVideoQuality(f);
			}

			mc.getTextureManager().bindTexture(buttonTextures);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
		}
	}
	
	private float snapValue(float val) {
		int i = Math.round(val*10);
		return i/10f;
	}

	private float normalizeValue(float val) {
		 return (val-0.1f)/0.8f;
	}
	
	private float denormalizeValue(float val) {
		//Transfers the value ranging from 0.0 to 1.0 to the scale of 0.1 to 0.9
		float r = 0.8f*val;
		return 0.1f+r;
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
