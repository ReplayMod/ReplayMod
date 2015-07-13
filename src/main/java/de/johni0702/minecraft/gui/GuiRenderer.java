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

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public interface GuiRenderer {

    ReadablePoint getOpenGlOffset();

    ReadableDimension getSize();

    void bindTexture(ResourceLocation location);

    void bindTexture(ITextureObject texture);

    void drawTexturedRect(int x, int y, int u, int v, int width, int height);

    void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight);

    void drawRect(int x, int y, int width, int height, int color);

    void drawRect(int x, int y, int width, int height, ReadableColor color);

    void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor);

    void drawRect(int x, int y, int width, int height, ReadableColor topLeftColor, ReadableColor topRightColor, ReadableColor bottomLeftColor, ReadableColor bottomRightColor);

    int drawString(int x, int y, int color, String text);

    int drawString(int x, int y, ReadableColor color, String text);

    int drawCenteredString(int x, int y, int color, String text);

    int drawCenteredString(int x, int y, ReadableColor color, String text);

    int drawString(int x, int y, int color, String text, boolean shadow);

    int drawString(int x, int y, ReadableColor color, String text, boolean shadow);

    int drawCenteredString(int x, int y, int color, String text, boolean shadow);

    int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow);

}
