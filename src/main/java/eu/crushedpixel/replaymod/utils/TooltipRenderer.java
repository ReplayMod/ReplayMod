package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.util.Point;

import java.awt.Color;

public class TooltipRenderer extends Gui {

    private final Minecraft mc = Minecraft.getMinecraft();

    public void drawTooltip(int x, int y, String text, GuiScreen parent, Color textColor) {
        drawTooltip(x, y, text, parent, textColor.getRGB());
    }

    public void drawTooltip(int x, int y, String text, GuiScreen parent, int textColor) {
        drawTooltip(x, y, StringUtils.splitStringInMultipleRows(text, 250), parent, textColor);
    }

    public void drawTooltip(int x, int y, String[] textLines, GuiScreen parent, int textColor) {
        int maxLineWidth = 0;

        int screenWidth, screenHeight;
        if(parent == null) {
            Point screenDimensions = MouseUtils.getScaledDimensions();
            screenWidth = screenDimensions.getX();
            screenHeight = screenDimensions.getY();
        } else {
            screenWidth = parent.width;
            screenHeight = parent.height;
        }

        for(String line : textLines) {
            int stringWidth = mc.fontRendererObj.getStringWidth(line);

            if(stringWidth > maxLineWidth) {
                maxLineWidth = stringWidth;
            }
        }

        int j2 = x + 12;
        int k2 = y - 12;
        int i1 = 8;

        if (textLines.length > 1) {
            i1 += (textLines.length - 1) * 12;
        }

        if (j2 + maxLineWidth > screenWidth) {
            j2 -= 28 + maxLineWidth;
        }

        if (k2 + i1 + 6 > screenHeight) {
            k2 = screenHeight - i1 - 6;
        }

        int j1 = -267386864;

        this.drawGradientRect(j2 - 3, k2 - 4, j2 + maxLineWidth + 3, k2 - 3, j1, j1);
        this.drawGradientRect(j2 - 3, k2 + i1 + 3, j2 + maxLineWidth + 3, k2 + i1 + 4, j1, j1);
        this.drawGradientRect(j2 - 3, k2 - 3, j2 + maxLineWidth + 3, k2 + i1 + 3, j1, j1);
        this.drawGradientRect(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        this.drawGradientRect(j2 + maxLineWidth + 3, k2 - 3, j2 + maxLineWidth + 4, k2 + i1 + 3, j1, j1);

        int k1 = 1347420415;
        int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;

        this.drawGradientRect(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        this.drawGradientRect(j2 + maxLineWidth + 2, k2 - 3 + 1, j2 + maxLineWidth + 3, k2 + i1 + 3 - 1, k1, l1);
        this.drawGradientRect(j2 - 3, k2 - 3, j2 + maxLineWidth + 3, k2 - 3 + 1, k1, k1);
        this.drawGradientRect(j2 - 3, k2 + i1 + 2, j2 + maxLineWidth + 3, k2 + i1 + 3, l1, l1);

        for(String line : textLines) {
            mc.fontRendererObj.drawStringWithShadow(line, j2, k2, textColor);

            k2 += 12;
        }
    }
}
