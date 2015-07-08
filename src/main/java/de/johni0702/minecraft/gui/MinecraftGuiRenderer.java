/*
 * Copyright (c) 2015 johni0702
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.johni0702.minecraft.gui;

import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.*;

public class MinecraftGuiRenderer implements GuiRenderer {

    private final Gui gui = new Gui();

    @NonNull
    private final ReadableDimension size;

    public MinecraftGuiRenderer(ReadableDimension size) {
        this.size = size;
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        return new Point(0, 0);
    }

    @Override
    public ReadableDimension getSize() {
        return size;
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        gui.drawTexturedModalRect(x, y, u, v, width, height);
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        return fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color(color), text);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        return fontRenderer.drawStringWithShadow(text, x - fontRenderer.getStringWidth(text) / 2, y, color);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color(color), text);
    }

    private int color(ReadableColor color) {
        return color.getAlpha() << 24
                | color.getRed() << 16
                | color.getGreen() << 8
                | color.getBlue();
    }
}
