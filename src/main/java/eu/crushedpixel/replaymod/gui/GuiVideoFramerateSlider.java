package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.gui.elements.GuiAdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

public class GuiVideoFramerateSlider extends GuiAdvancedButton {

    public boolean dragging;
    private String displayKey;
    private float sliderValue;
    public GuiVideoFramerateSlider(int xPos, int yPos, int initialFramerate, String displayKey) {
        super(xPos, yPos, 150, 20, "", null, null);
        this.sliderValue = normalizeValue(initialFramerate);
        this.displayString = displayKey + ": " + translate(initialFramerate);
        this.displayKey = displayKey;
    }

    private String translate(int value) {
        return String.valueOf(value);
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
        if(this.visible) {
            if(this.dragging) {
                sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                sliderValue = MathHelper.clamp_float(sliderValue, 0.0F, 1.0F);
                int f = denormalizeValue(sliderValue);
                this.displayString = displayKey + ": " + translate(f);
            }

            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition + (int) (sliderValue * (float) (this.width - 8)), this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.xPosition + (int) (sliderValue * (float) (this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
        }
    }

    public int getFPS() {
        return denormalizeValue(sliderValue);
    }

    private float normalizeValue(int val) {
        return (val - 10) / 110f;
    }

    private int denormalizeValue(float val) {
        //Transfers the value ranging from 0.0 to 1.0 to the scale of 10 to 120
        float r = 110f * val;
        return Math.round(10 + r);
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if(super.mousePressed(mc, mouseX, mouseY)) {
            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
        this.dragging = false;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        mouseDrag(mc, mouseX, mouseY, 0);
    }
}
