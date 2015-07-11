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

package de.johni0702.minecraft.gui.element.advanced;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.AbstractGuiElement;
import lombok.Getter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;

public abstract class AbstractGuiProgressBar<T extends AbstractGuiElement<T>> extends AbstractGuiElement<T> implements IGuiProgressBar<T> {
    private static final int BORDER = 2;

    @Getter
    private float progress;

    @Getter
    private String label = "%d%%";

    public AbstractGuiProgressBar() {
    }

    public AbstractGuiProgressBar(GuiContainer container) {
        super(container);
    }

    @Override
    public T setProgress(float progress) {
        this.progress = progress;
        return getThis();
    }

    @Override
    public T setLabel(String label) {
        this.label = label;
        return getThis();
    }

    @Override
    public T setI18nLabel(String label) {
        return setLabel(I18n.format(label));
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        int width = size.getWidth();
        int height = size.getHeight();
        int barTotalWidth = width - 2 * BORDER;
        int barDoneWidth = (int) (barTotalWidth * progress);

        renderer.drawRect(0, 0, width, height, ReadableColor.BLACK); // Border
        renderer.drawRect(BORDER, BORDER, barTotalWidth, height - 2 * BORDER, ReadableColor.WHITE); // Background
        renderer.drawRect(BORDER, BORDER, barDoneWidth, height - 2 * BORDER, ReadableColor.GREY); // Progress

        String text = String.format(label, (int)(progress * 100));
        renderer.drawCenteredString(width / 2, size.getHeight() / 2 - fontRenderer.FONT_HEIGHT / 2, ReadableColor.BLACK, text);
    }

    @Override
    public ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }
}
