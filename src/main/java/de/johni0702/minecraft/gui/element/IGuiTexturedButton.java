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

package de.johni0702.minecraft.gui.element;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public interface IGuiTexturedButton<T extends IGuiTexturedButton<T>> extends IGuiClickable<T> {
    ResourceLocation getTexture();
    ReadableDimension getTextureTotalSize();
    T setTexture(ResourceLocation resourceLocation, int size);
    T setTexture(ResourceLocation resourceLocation, int width, int height);

    ReadableDimension getTextureSize();
    T setTextureSize(int size);
    T setTextureSize(int width, int height);

    ReadablePoint getTextureNormal();
    ReadablePoint getTextureHover();
    ReadablePoint getTextureDisabled();
    T setTexturePosH(int x, int y);
    T setTexturePosV(int x, int y);
    T setTexturePosH(ReadablePoint pos);
    T setTexturePosV(ReadablePoint pos);
    T setTexturePos(int normalX, int normalY, int hoverX, int hoverY);
    T setTexturePos(ReadablePoint normal, ReadablePoint hover);
    T setTexturePos(int normalX, int normalY, int hoverX, int hoverY, int disabledX, int disabledY);
    T setTexturePos(ReadablePoint normal, ReadablePoint hover, ReadablePoint disabled);
}
