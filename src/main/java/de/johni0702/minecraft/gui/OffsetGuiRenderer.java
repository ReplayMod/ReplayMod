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
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public class OffsetGuiRenderer implements GuiRenderer {

    @NonNull
    private final GuiRenderer renderer;

    @NonNull
    private final ReadablePoint position;

    @NonNull
    private final ReadableDimension size;

    public OffsetGuiRenderer(GuiRenderer renderer, ReadablePoint position, ReadableDimension size) {
        this.renderer = renderer;
        this.position = position;
        this.size = size;
        if (size.getHeight() > renderer.getSize().getHeight()
                || size.getWidth() > renderer.getSize().getWidth()) {
            throw new IllegalArgumentException("Size must not be larger than size of renderer.");
        }
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        ReadablePoint parentOffset = renderer.getOpenGlOffset();
        return new Point(parentOffset.getX() + position.getX(), parentOffset.getY() + position.getY());
    }

    @Override
    public ReadableDimension getSize() {
        return size;
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        renderer.bindTexture(location);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        renderer.drawTexturedRect(x + position.getX(), y + position.getY(), u, v, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        renderer.drawRect(x + position.getX(), y + position.getY(), width, height, color);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor color) {
        renderer.drawRect(x + position.getX(), y + position.getY(), width, height, color);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        renderer.drawRect(x + position.getX(), y + position.getY(), width, height, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor topLeftColor, ReadableColor topRightColor, ReadableColor bottomLeftColor, ReadableColor bottomRightColor) {
        renderer.drawRect(x + position.getX(), y + position.getY(), width, height, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return renderer.drawString(x + position.getX(), y + position.getY(), color, text) - position.getX();
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return renderer.drawString(x + position.getX(), y + position.getY(), color, text) - position.getX();
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return renderer.drawCenteredString(x + position.getX(), y + position.getY(), color, text) - position.getX();
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return renderer.drawCenteredString(x + position.getX(), y + position.getY(), color, text) - position.getX();
    }
}
