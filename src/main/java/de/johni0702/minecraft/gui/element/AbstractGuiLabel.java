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
import lombok.Getter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;

public abstract class AbstractGuiLabel<T extends AbstractGuiLabel<T>> extends AbstractGuiElement<T> implements IGuiLabel<T> {
    @Getter
    private String text = "";

    @Getter
    private ReadableColor color = ReadableColor.WHITE, disabledColor = ReadableColor.GREY;

    public AbstractGuiLabel() {
    }

    public AbstractGuiLabel(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        renderer.drawString(0, 0, isEnabled() ? color : disabledColor, text);
    }

    @Override
    public ReadableDimension getMinSize() {
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        return new Dimension(fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT);
    }

    @Override
    public T setText(String text) {
        this.text = text;
        return getThis();
    }

    @Override
    public T setI18nText(String text, Object... args) {
        return setText(I18n.format(text, args));
    }

    @Override
    public T setColor(ReadableColor color) {
        this.color = color;
        return getThis();
    }

    @Override
    public T setDisabledColor(ReadableColor disabledColor) {
        this.disabledColor = disabledColor;
        return getThis();
    }
}
