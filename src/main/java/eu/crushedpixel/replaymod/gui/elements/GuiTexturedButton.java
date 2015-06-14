package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiTexturedButton extends GuiButton implements GuiElement {
    private final ResourceLocation texture;
    private final int u, v;
    private final int textureWidth, textureHeight;
    private final Runnable action;
    private final String hoverText;

    public GuiTexturedButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                             int u, int v, int textureWidth, int textureHeight, Runnable action) {
        this(buttonId, x, y, width, height, texture, u, v, textureWidth, textureHeight, action, null);
    }

    public GuiTexturedButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                             int u, int v, int textureWidth, int textureHeight, Runnable action, String hoverText) {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.action = action;
        this.hoverText = hoverText;
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return enabled && visible
                && mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            hovered = isHovering(mouseX, mouseY);

            mc.renderEngine.bindTexture(texture);

            GlStateManager.color(1, 1, 1);
            int u = this.u + (hovered ? width : 0);
            Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition, u, v, width, height, textureWidth, textureHeight);
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        drawButton(mc, mouseX, mouseY);
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        hovered = isHovering(mouseX, mouseY);
        if(hovered && hoverText != null) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, hoverText, null, Color.WHITE);
        }
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if (isHovering(mouseX, mouseY)) {
            performAction();
        }
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

    public void performAction() {
        action.run();
    }
}
