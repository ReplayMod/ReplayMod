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

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.util.ReadableDimension;

import java.awt.image.BufferedImage;

public abstract class AbstractGuiImage<T extends AbstractGuiImage<T>>
        extends AbstractGuiElement<T> implements IGuiImage<T> {
    private DynamicTexture texture;
    private int textureWidth, textureHeight;

    public AbstractGuiImage() {
    }

    public AbstractGuiImage(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        renderer.bindTexture(texture);
        int w = size.getWidth();
        int h = size.getHeight();
        renderer.drawTexturedRect(0, 0, 0, 0, w, h, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (texture != null) {
            getMinecraft().addScheduledTask(new Finalizer(texture));
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return getPreferredSize();
    }

    @Override
    public T setTexture(BufferedImage img) {
        if (texture != null) {
            texture.deleteGlTexture();
        }
        texture = new DynamicTexture(img);
        textureWidth = img.getWidth();
        textureHeight = img.getHeight();
        return getThis();
    }

    /**
     * We use a static class here in order to prevent the inner class from keeping the outer class
     * alive after finalization when still unloading the texture.
     */
    @RequiredArgsConstructor
    private static final class Finalizer implements Runnable {
        private final DynamicTexture texture;

        @Override
        public void run() {
            texture.deleteGlTexture();
        }
    }
}
