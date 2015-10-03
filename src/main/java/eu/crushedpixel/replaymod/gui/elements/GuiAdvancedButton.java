package eu.crushedpixel.replaymod.gui.elements;

import com.replaymod.core.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

public class GuiAdvancedButton extends GuiButton implements GuiElement {
    private final Runnable action;
    public String hoverText;

    public GuiAdvancedButton(int id, int x, int y, String buttonText) {
        this(id, x, y, buttonText, null, null);
    }

    public GuiAdvancedButton(int x, int y, int width, int height, String buttonText, Runnable action, String hoverText) {
        this(0, x, y, width, height, buttonText, action, hoverText);
    }

    public GuiAdvancedButton(int id, int x, int y, String buttonText, Runnable action, String hoverText) {
        super(id, x, y, buttonText);
        this.action = action;
        this.hoverText = hoverText;
    }

    public GuiAdvancedButton(int id, int x, int y, int width, int height, String buttonText, Runnable action, String hoverText) {
        super(id, x, y, width, height, buttonText);
        this.action = action;
        this.hoverText = hoverText;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        this.draw(mc, mouseX, mouseY, isHovering(mouseX, mouseY));
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovering) {
        if (this.visible) {
            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = isHovering(mouseX, mouseY);
            int k = this.getHoverState(hovering);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.blendFunc(770, 771);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + k * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + k * 20, this.width / 2, this.height);
            this.mouseDragged(mc, mouseX, mouseY);
            int l = 14737632;

            if (packedFGColour != 0) {
                l = packedFGColour;
            } else if (!this.enabled) {
                l = 10526880;
            } else if (hovering) {
                l = 16777120;
            }

            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, l);
        }
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        hovered = isHovering(mouseX, mouseY);
        if(hovered && !StringUtils.isEmpty(hoverText)) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, hoverText, null, Color.WHITE);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX <= xPosition + width
                && mouseY <= yPosition + height;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if (isHovering(mouseX, mouseY) && enabled) {
            performAction();
            return true;
        }
        return false;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {

    }

    @Override
    public void tick(Minecraft mc) {

    }

    @Override
    public void setElementEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void performAction() {
        if (action != null) {
            action.run();
        }
    }

    @Override
    public int xPos() {
        return xPosition;
    }

    @Override
    public int yPos() {
        return yPosition;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void xPos(int x) {
        xPosition = x;
    }

    @Override
    public void yPos(int y) {
        yPosition = y;
    }

    @Override
    public void width(int width) {
        this.width = width;
    }

    @Override
    public void height(int height) {
        this.height = height;
    }
}
