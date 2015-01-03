package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import eu.crushedpixel.replaymod.ReplayMod;

public class GuiSizeLimitOptionSlider extends GuiButton {

	public GuiSizeLimitOptionSlider(int buttonId, int p_i45017_2_, int p_i45017_3_, float valueMin, float valueMax, float valueStep, int sliderValue, String displayKey)
	{
		super(buttonId, p_i45017_2_, p_i45017_3_, 150, 20, "");
		this.valueMin = valueMin;
		this.valueMax = valueMax;
		this.valueStep = valueStep;
		this.sliderValue = sliderValue;
		this.displayString = displayKey+": "+translate(sliderValue);
		this.displayKey = displayKey;
		
		float val = realToNormalized(convertScaleRet(sliderValue));
		

		this.sliderValue = val;
	}

	private float sliderValue;
	public boolean dragging;
	private GameSettings.Options options;

	private float valueStep, valueMin, valueMax;
	private String displayKey;


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
				sliderValue = normalizeValue(f);
				this.displayString = displayKey+": "+translate(convertScale(normalizedToReal(sliderValue)));
				ReplayMod.replaySettings.setMaximumFileSize(convertScale(normalizedToReal(sliderValue)));
			}

			mc.getTextureManager().bindTexture(buttonTextures);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
			this.drawTexturedModalRect(this.xPosition + (int)(sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
		}
	}

	public static String translate(int value) {
		if(value == 0) {
			return "Unlimited";
		} else {
			return convertToStringRepresentation(value);
		}
	}
	
	public static int convertScale(float value) {
		if(value == 0) {
			return 0;
		} else {
			if(value > 20) {
				value = 1024+((value-20)*1024);
			} else {
				value = value*50;
			}
			if(value == 1000) {
				value = 1024;
			}
			return Math.round(value);
		}
	}
	
	public static int convertScaleRet(float value) {
		if(value == 0) {
			return 0;
		} else {
			if(value >= 1024) {
				value = 20+((value-1024)/1024);
			} else {
				value = value/50;
			}
			if(value == 1024) {
				value = 1000;
			}
			return Math.round(value);
		}
	}

	public static String convertToStringRepresentation(final long value){
		long M = 1;
		long G = 1024;
		long T = G * 1024;

		final long[] dividers = new long[] { T, G, M};
		final String[] units = new String[] { "TB", "GB", "MB"};
		if(value < 1)
			throw new IllegalArgumentException("Invalid file size: " + value);
		String result = null;
		for(int i = 0; i < dividers.length; i++){
			final long divider = dividers[i];
			if(value >= divider){
				result = format(value, divider, units[i]);
				break;
			}
		}
		return result;
	}

	private static String format(final long value,
			final long divider,
			final String unit){
		final double result =
				divider > 1 ? (double) value / (double) divider : (double) value;
				return String.format("%.0f %s", Double.valueOf(result), unit);
	}

	public float normalizedToReal(float value) {
		float min = 0 - valueMin;
		float max = valueMax + min;

		return Math.round(value*(max) - min);
	}

	public float realToNormalized(float value) {
		float min = 0 - valueMin;
		float max = valueMax + min;

		return value/(max) - min;
	}
	
	public float normalizeValue(float p_148266_1_)
	{
		return MathHelper.clamp_float((this.snapToStepClamp(p_148266_1_) - this.valueMin) / (this.valueMax - this.valueMin), 0.0F, 1.0F);
	}

	public float denormalizeValue(float p_148262_1_)
	{
		return this.snapToStepClamp(this.valueMin + (this.valueMax - this.valueMin) * MathHelper.clamp_float(p_148262_1_, 0.0F, 1.0F));
	}

	public float snapToStepClamp(float p_148268_1_)
	{
		p_148268_1_ = this.snapToStep(p_148268_1_);
		return MathHelper.clamp_float(p_148268_1_, this.valueMin, this.valueMax);
	}

	protected float snapToStep(float p_148264_1_)
	{
		if (this.valueStep > 0.0F)
		{
			p_148264_1_ = this.valueStep * (float)Math.round(p_148264_1_ / this.valueStep);
		}

		return p_148264_1_;
	}

	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
	{
		if (super.mousePressed(mc, mouseX, mouseY))
		{
			this.dragging = true;
			return true;
		}
		else
		{
			return false;
		}
	}

	public void mouseReleased(int mouseX, int mouseY)
	{
		this.dragging = false;
	}
}
