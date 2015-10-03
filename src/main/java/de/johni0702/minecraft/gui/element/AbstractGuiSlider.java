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
import de.johni0702.minecraft.gui.function.Clickable;
import de.johni0702.minecraft.gui.function.Draggable;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

// TODO: Currently assumes a height of 20
public abstract class AbstractGuiSlider<T extends AbstractGuiSlider<T>> extends AbstractGuiElement<T> implements Clickable, Draggable, IGuiSlider<T> {
    private Runnable onValueChanged;
    private ReadableDimension size;

    private int value;
    private int steps;

    private String text = "";

    private boolean dragging;

    public AbstractGuiSlider() {
    }

    public AbstractGuiSlider(GuiContainer container) {
        super(container);
    }

    @Override
    protected ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        Point pos = new Point(position);
        if (getContainer() != null) {
            getContainer().convertFor(this, pos);
        }

        if (isMouseHovering(pos) && isEnabled()) {
            updateValue(pos);
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDrag(ReadablePoint position, int button, long timeSinceLastCall) {
        if (dragging) {
            Point pos = new Point(position);
            if (getContainer() != null) {
                getContainer().convertFor(this, pos);
            }
            updateValue(pos);
        }
        return dragging;
    }

    @Override
    public boolean mouseRelease(ReadablePoint position, int button) {
        if (dragging) {
            dragging = false;
            Point pos = new Point(position);
            if (getContainer() != null) {
                getContainer().convertFor(this, pos);
            }
            updateValue(pos);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isMouseHovering(ReadablePoint pos) {
        return pos.getX() > 0 && pos.getY() > 0
                && pos.getX() < size.getWidth() && pos.getY() < size.getHeight();
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        this.size = size;

        int width = size.getWidth();
        int height = size.getHeight();

        renderer.bindTexture(GuiButton.WIDGETS_TEXTURE);

        // Draw background
        renderer.drawTexturedRect(0, 0, 0, 46, width / 2, height);
        renderer.drawTexturedRect(width / 2, 0, 200 - width / 2, 46, width / 2, height);

        // Draw slider
        int sliderX = (width - 8) * value / steps;
        renderer.drawTexturedRect(sliderX, 0, 0, 66, 4, 20);
        renderer.drawTexturedRect(sliderX + 4, 0, 196, 66, 4, 20);

        // Draw text
        int color = 0xe0e0e0;
        if (!isEnabled()) {
            color = 0xa0a0a0;
        } else if (isMouseHovering(new Point(renderInfo.mouseX, renderInfo.mouseY))) {
            color = 0xffffa0;
        }
        renderer.drawCenteredString(width / 2, height / 2 - 4, color, text);
    }

    protected void updateValue(ReadablePoint position) {
        if (size == null) {
            return;
        }
        int width = size.getWidth() - 8;
        int pos = Math.max(0, Math.min(width, position.getX() - 4));
        setValue(steps * pos / width);
    }

    public void onValueChanged() {
        if (onValueChanged != null) {
            onValueChanged.run();
        }
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
    public T setValue(int value) {
        this.value = value;
        onValueChanged();
        return getThis();
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public int getSteps() {
        return steps;
    }

    @Override
    public T setSteps(int steps) {
        this.steps = steps;
        return getThis();
    }

    @Override
    public T onValueChanged(Runnable runnable) {
        this.onValueChanged = runnable;
        return getThis();
    }
}
