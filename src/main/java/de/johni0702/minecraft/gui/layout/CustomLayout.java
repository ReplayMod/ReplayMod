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

package de.johni0702.minecraft.gui.layout;

import com.google.common.collect.Maps;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.GuiElement;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.util.Collection;
import java.util.Map;

public abstract class CustomLayout<T extends GuiContainer<T>> implements Layout {
    private final Layout parent;
    private Map<GuiElement, Pair<Point, Dimension>> result = Maps.newHashMap();

    public CustomLayout() {
        this(null);
    }

    public CustomLayout(Layout parent) {
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layOut(GuiContainer container, ReadableDimension size) {
        result.clear();
        if (parent == null) {
            Collection<GuiElement> elements = container.getChildren();
            for (GuiElement element : elements) {
                result.put(element, Pair.of(new Point(0, 0), new Dimension(element.getMinSize())));
            }
        } else {
            Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> elements = parent.layOut(container, size);
            for (Map.Entry<GuiElement, Pair<ReadablePoint, ReadableDimension>> entry : elements.entrySet()) {
                Pair<ReadablePoint, ReadableDimension> pair = entry.getValue();
                result.put(entry.getKey(), Pair.of(new Point(pair.getLeft()), new Dimension(pair.getRight())));
            }
        }

        layout((T) container, size.getWidth(), size.getHeight());

        return (Map) result;
    }

    private Pair<Point, Dimension> entry(GuiElement element) {
        return result.get(element);
    }

    protected void set(GuiElement element, int x, int y, int width, int height) {
        Pair<Point, Dimension> entry = entry(element);
        entry.getLeft().setLocation(x, y);
        entry.getRight().setSize(width, height);
    }

    protected void pos(GuiElement element, int x, int y) {
        entry(element).getLeft().setLocation(x, y);
    }

    protected void size(GuiElement element, ReadableDimension size) {
        size.getSize(entry(element).getRight());
    }

    protected void size(GuiElement element, int width, int height) {
        entry(element).getRight().setSize(width, height);
    }

    protected void x(GuiElement element, int x) {
        entry(element).getLeft().setX(x);
    }

    protected void y(GuiElement element, int y) {
        entry(element).getLeft().setY(y);
    }

    protected void width(GuiElement element, int width) {
        entry(element).getRight().setWidth(width);
    }

    protected void height(GuiElement element, int height) {
        entry(element).getRight().setHeight(height);
    }

    protected int x(GuiElement element) {
        return entry(element).getLeft().getX();
    }

    protected int y(GuiElement element) {
        return entry(element).getLeft().getY();
    }

    protected int width(GuiElement element) {
        return entry(element).getRight().getWidth();
    }

    protected int height(GuiElement element) {
        return entry(element).getRight().getHeight();
    }

    protected abstract void layout(T container, int width, int height);

    @Override
    public ReadableDimension calcMinSize(GuiContainer<?> container) {
        return new Dimension(0, 0);
    }
}
