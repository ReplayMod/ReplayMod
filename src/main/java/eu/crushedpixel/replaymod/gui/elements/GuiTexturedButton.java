package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiTexturedButton extends GuiAdvancedButton implements GuiElement {
    private final ResourceLocation texture;
    private final int u, v;
    private final int textureWidth, textureHeight;

    public GuiTexturedButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                             int u, int v, int textureWidth, int textureHeight, Runnable action, String hoverText) {
        super(buttonId, x, y, width, height, "", action, hoverText);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovering) {
        if (visible) {
            hovered = isHovering(mouseX, mouseY) && enabled;

            if(!enabled) {
                GlStateManager.color(Color.GRAY.getRed() / 255f, Color.GRAY.getGreen() / 255f, Color.GRAY.getBlue() / 255f, 1f);
            } else {
                GlStateManager.color(1f, 1f, 1f);
            }

            mc.renderEngine.bindTexture(texture);

            int u = this.u + (hovering ? width : 0);
            Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition, u, v, width, height, textureWidth, textureHeight);
        }
    }
}
