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

package de.johni0702.minecraft.gui.container;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.OffsetGuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.function.Scrollable;
import org.lwjgl.util.*;

public abstract class AbstractGuiScrollable<T extends AbstractGuiScrollable<T>> extends AbstractGuiContainer<T>
        implements Scrollable {
    private int offsetX, offsetY;
    private final ReadablePoint negativeOffset = new ReadablePoint() {
        @Override
        public int getX() {
            return -offsetX;
        }

        @Override
        public int getY() {
            return -offsetY;
        }

        @Override
        public void getLocation(WritablePoint dest) {
            dest.setLocation(getX(), getY());
        }
    };

    private Direction scrollDirection = Direction.VERTICAL;

    protected ReadableDimension lastRenderSize;

    public AbstractGuiScrollable() {
    }

    public AbstractGuiScrollable(GuiContainer container) {
        super(container);
    }

    @Override
    public void convertFor(GuiElement element, Point point) {
        super.convertFor(element, point);
        if (point.getX() > 0 && point.getX() < lastRenderSize.getWidth()
                 && point.getY() > 0 && point.getY() < lastRenderSize.getHeight()) {
            point.translate(offsetX, offsetY);
        } else {
            point.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        int width = size.getWidth();
        int height = size.getHeight();
        lastRenderSize = size;
        size = super.calcMinSize();
        size = new Dimension(Math.max(width, size.getWidth()), Math.max(height, size.getHeight()));
        renderInfo = renderInfo.offsetMouse(-offsetX, -offsetY);

        OffsetGuiRenderer offsetRenderer = new OffsetGuiRenderer(renderer, negativeOffset, size);
        offsetRenderer.startUsing();
        super.draw(offsetRenderer, size, renderInfo);
        offsetRenderer.stopUsing();
    }

    @Override
    public ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public boolean scroll(ReadablePoint mousePosition, int dWheel) {
        Point mouse = new Point(mousePosition);
        if (getContainer() != null) {
            getContainer().convertFor(this, mouse);
        }
        if (mouse.getX() > 0 && mouse.getY() > 0
                && mouse.getX() < lastRenderSize.getWidth() && mouse.getY() < lastRenderSize.getHeight()) {
            // Reduce scrolling speed but make sure it is never rounded to 0
            dWheel = (int) Math.copySign(Math.ceil(Math.abs(dWheel) / 4.0), dWheel);
            if (scrollDirection == Direction.HORIZONTAL) {
                scrollX(dWheel);
            } else {
                scrollY(dWheel);
            }
            return true;
        }
        return false;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public T setOffsetX(int offsetX) {
        this.offsetX = offsetX;
        return getThis();
    }

    public int getOffsetY() {
        return offsetY;
    }

    public T setOffsetY(int offsetY) {
        this.offsetY = offsetY;
        return getThis();
    }

    public Direction getScrollDirection() {
        return scrollDirection;
    }

    public T setScrollDirection(Direction scrollDirection) {
        this.scrollDirection = scrollDirection;
        return getThis();
    }

    public T scrollX(int dPixel) {
        offsetX = Math.max(0, Math.min(super.calcMinSize().getWidth() - lastRenderSize.getWidth(), offsetX - dPixel));
        return getThis();
    }

    public T scrollY(int dPixel) {
        offsetY = Math.max(0, Math.min(super.calcMinSize().getHeight() - lastRenderSize.getHeight(), offsetY - dPixel));
        return getThis();
    }

    public enum Direction {
        VERTICAL, HORIZONTAL;
    }
}
