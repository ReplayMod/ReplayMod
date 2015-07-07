package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

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
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            hovered = isHovering(mouseX, mouseY);

            mc.renderEngine.bindTexture(texture);

            GlStateManager.color(1, 1, 1);
            int u = this.u + (hovered ? width : 0);
            Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition, u, v, width, height, textureWidth, textureHeight);
        }
    }
}
