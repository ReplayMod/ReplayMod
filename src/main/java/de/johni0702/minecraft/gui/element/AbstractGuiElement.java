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

import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;

public abstract class AbstractGuiElement<T extends AbstractGuiElement<T>> implements GuiElement<T> {
    @Getter
    private final Minecraft minecraft = Minecraft.getMinecraft();

    @Getter
    private GuiContainer container;

    private GuiElement tooltip;

    @Getter
    private boolean enabled = true;

    private Dimension maxSize, preferredSize;

    public AbstractGuiElement() {
    }

    public AbstractGuiElement(GuiContainer container) {
        container.addElements(null, this);
    }

    protected abstract T getThis();

    @Override
    public T setEnabled(boolean enabled) {
        this.enabled = enabled;
        return getThis();
    }

    @Override
    public T setEnabled() {
        return setEnabled(true);
    }

    @Override
    public T setDisabled() {
        return setEnabled(false);
    }

    @Override
    public GuiElement getTooltip(RenderInfo renderInfo) {
        if (tooltip != null) {
            Point mouse = new Point(renderInfo.mouseX, renderInfo.mouseY);
            if (container != null) {
                container.convertFor(this, mouse);
            }
            ReadableDimension size = getMinSize();
            if (mouse.getX() > 0
                    && mouse.getY() > 0
                    && mouse.getX() < size.getWidth()
                    && mouse.getY() < size.getHeight()) {
                return tooltip;
            }
        }
        return null;
    }

    @Override
    public T setTooltip(GuiElement tooltip) {
        this.tooltip = tooltip;
        return getThis();
    }

    @Override
    public T setContainer(GuiContainer container) {
        this.container = container;
        return getThis();
    }

    public T setMaxSize(ReadableDimension maxSize) {
        this.maxSize = new Dimension(maxSize);
        return getThis();
    }

    public T setPreferredSize(ReadableDimension preferredSize) {
        this.preferredSize = new Dimension(preferredSize);
        return getThis();
    }

    public T setSize(ReadableDimension size) {
        Dimension dimension = new Dimension(size);
        maxSize = preferredSize = dimension;
        return getThis();
    }

    public T setSize(int width, int height) {
        return setSize(new Dimension(width, height));
    }

    public T setWidth(int width) {
        if (maxSize == null) {
            maxSize = new Dimension(width, 0);
        } else {
            maxSize.setWidth(width);
        }
        if (preferredSize == null) {
            preferredSize = new Dimension(width, 0);
        } else {
            preferredSize.setWidth(width);
        }
        return getThis();
    }

    public T setHeight(int height) {
        if (maxSize == null) {
            maxSize = new Dimension(0, height);
        } else {
            maxSize.setHeight(height);
        }
        if (preferredSize == null) {
            preferredSize = new Dimension(0, height);
        } else {
            preferredSize.setHeight(height);
        }
        return getThis();
    }

    public int getLayer() {
        return 0;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return maxSize == null ? getPreferredSize() : maxSize;
    }

    @Override
    public ReadableDimension getPreferredSize() {
        return preferredSize == null ? getMinSize() : preferredSize;
    }
}
