package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;

public class GuiVideoQualitySlider extends GuiButton {

    public boolean dragging;
    private String displayKey;
    private float sliderValue;
    public GuiVideoQualitySlider(int buttonId, int p_i45017_2_, int p_i45017_3_, float d, String displayKey) {
        super(buttonId, p_i45017_2_, p_i45017_3_, 150, 20, "");
        this.sliderValue = normalizeValue(d);
        this.displayString = displayKey + ": " + translate(d);
        this.displayKey = displayKey;
    }

    private final String DRAFT = I18n.format("replaymod.gui.settings.videoquality.draft");
    private final String NORMAL = I18n.format("replaymod.gui.settings.videoquality.normal");
    private final String GOOD= I18n.format("replaymod.gui.settings.videoquality.good");
    private final String BEST = I18n.format("replaymod.gui.settings.videoquality.best");

    private String translate(float value) {
        if(value <= 0.3) {
            return DRAFT;
        } else if(value <= 0.5) {
            return NORMAL;
        } else if(value <= 0.7) {
            return GOOD;
        }
        return BEST;
    }

    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if(this.visible) {
            if(this.dragging) {
                sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                sliderValue = MathHelper.clamp_float(sliderValue, 0.0F, 1.0F);
                float f = denormalizeValue(sliderValue);
                f = snapValue(f);
                sliderValue = normalizeValue(f);
                this.displayString = displayKey + ": " + translate(f);
            }

            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition +  (int)Math.ceil(sliderValue * (float) (this.width - 8)), this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.xPosition + (int)Math.ceil(sliderValue * (float) (this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
        }
    }

    public float getQuality() {
        return snapValue(denormalizeValue(sliderValue));
    }

    private float snapValue(float val) {
        int i = Math.round(val * 10);
        return i / 10f;
    }

    private float normalizeValue(float val) {
        return (val - 0.1f) / 0.8f;
    }

    private float denormalizeValue(float val) {
        //Transfers the value ranging from 0.0 to 1.0 to the scale of 0.1 to 0.9
        float r = 0.8f * val;
        return 0.1f + r;
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if(super.mousePressed(mc, mouseX, mouseY)) {
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
