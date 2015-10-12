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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.*;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class MinecraftGuiRenderer implements GuiRenderer {

    private final Gui gui = new Gui();

    @NonNull
    private final ScaledResolution size;

    public MinecraftGuiRenderer(ScaledResolution size) {
        this.size = size;
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        return new Point(0, 0);
    }

    @Override
    public ReadableDimension getSize() {
        return new ReadableDimension() {
            @Override
            public int getWidth() {
                return size.getScaledWidth();
            }

            @Override
            public int getHeight() {
                return size.getScaledHeight();
            }

            @Override
            public void getSize(WritableDimension dest) {
                dest.setSize(getWidth(), getHeight());
            }
        };
    }

    @Override
    public void setDrawingArea(int x, int y, int width, int height) {
        // glScissor origin is bottom left corner whereas otherwise it's top left
        y = size.getScaledHeight() - y - height;

        int f = size.getScaleFactor();
        GL11.glScissor(x * f, y * f, width * f, height * f);
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
    }

    @Override
    public void bindTexture(ITextureObject texture) {
        GlStateManager.bindTexture(texture.getGlTextureId());
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        gui.drawTexturedModalRect(x, y, u, v, width, height);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        GlStateManager.color(1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, textureWidth, textureHeight);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
        GlStateManager.color(1, 1, 1);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor color) {
        drawRect(x, y, width, height, color(color));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        drawRect(x, y, width, height, color(topLeftColor), color(topRightColor), color(bottomLeftColor), color(bottomRightColor));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br) {
        disableTexture2D();
        enableBlend();
        disableAlpha();
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        shadeModel(GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.startDrawingQuads();
        renderer.setColorRGBA(bl.getRed(), bl.getGreen(), bl.getBlue(), bl.getAlpha());
        renderer.addVertex(x, y + height, 0);
        renderer.setColorRGBA(br.getRed(), br.getGreen(), br.getBlue(), br.getAlpha());
        renderer.addVertex(x + width, y + height, 0);
        renderer.setColorRGBA(tr.getRed(), tr.getGreen(), tr.getBlue(), tr.getAlpha());
        renderer.addVertex(x + width, y, 0);
        renderer.setColorRGBA(tl.getRed(), tl.getGreen(), tl.getBlue(), tl.getAlpha());
        renderer.addVertex(x, y, 0);
        tessellator.draw();
        shadeModel(GL_FLAT);
        disableBlend();
        enableAlpha();
        enableTexture2D();
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return drawString(x, y, color, text, false);
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color(color), text);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return drawCenteredString(x, y, color, text, false);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color(color), text);
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int ret = shadow ? fontRenderer.drawStringWithShadow(text, x, y, color) : fontRenderer.drawString(text, x, y, color);
        GlStateManager.color(1, 1, 1);
        return ret;
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawString(x, y, color(color), text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        x-=fontRenderer.getStringWidth(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawCenteredString(x, y, color(color), text, shadow);
    }

    private int color(ReadableColor color) {
        return color.getAlpha() << 24
                | color.getRed() << 16
                | color.getGreen() << 8
                | color.getBlue();
    }

    private ReadableColor color(int color) {
        return new Color((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, (color >> 24) & 0xff);
    }
}
