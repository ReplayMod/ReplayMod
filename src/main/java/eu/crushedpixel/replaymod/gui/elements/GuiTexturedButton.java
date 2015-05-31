package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiTexturedButton extends GuiButton {
    private final ResourceLocation texture;
    private final int u, v;
    private final int textureWidth, textureHeight;
    private final String hoverKey;

    public GuiTexturedButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                             int u, int v, int textureWidth, int textureHeight) {
        this(buttonId, x, y, width, height, texture, u, v, textureWidth, textureHeight, null);
    }

    public GuiTexturedButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                             int u, int v, int textureWidth, int textureHeight, String hoverKey) {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.hoverKey = hoverKey;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            hovered = mouseX >= xPosition
                    && mouseY >= yPosition
                    && mouseX < xPosition + width
                    && mouseY < yPosition + height;

            mc.renderEngine.bindTexture(texture);

            GlStateManager.color(1, 1, 1);
            int u = this.u + (hovered ? width : 0);
            Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition, u, v, width, height, textureWidth, textureHeight);

            if(hovered && hoverKey != null) {
                ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format(hoverKey), null, Color.WHITE.getRGB());
            }
        }
    }
}
