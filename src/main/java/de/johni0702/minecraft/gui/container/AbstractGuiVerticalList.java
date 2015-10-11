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
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.function.Draggable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import lombok.Getter;
import org.lwjgl.util.*;

import static de.johni0702.minecraft.gui.utils.Colors.TRANSPARENT;
import static org.lwjgl.util.ReadableColor.BLACK;

public abstract class AbstractGuiVerticalList<T extends AbstractGuiVerticalList<T>> extends AbstractGuiScrollable<T>
        implements Draggable {
    public static final ReadableColor BACKGROUND = new Color(0, 0, 0, 150);

    @Getter
    private final VerticalLayout listLayout = new VerticalLayout().setSpacing(3);

    @Getter
    private final GuiPanel listPanel = new GuiPanel(this).setLayout(listLayout);

    {
        setLayout(new CustomLayout<T>() {
            @Override
            protected void layout(T container, int width, int height) {
                pos(listPanel, width / 2 - width(listPanel) / 2, 5);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                final ReadableDimension panelSize = listPanel.getMinSize();
                return new ReadableDimension() {
                    @Override
                    public int getWidth() {
                        return panelSize.getWidth();
                    }

                    @Override
                    public int getHeight() {
                        return panelSize.getHeight() + 10;
                    }

                    @Override
                    public void getSize(WritableDimension dest) {
                        dest.setSize(getWidth(), getHeight());
                    }
                };
            }
        });
    }

    private boolean drawShadow, drawSlider;

    private ReadablePoint lastMousePos;
    private boolean draggingSlider;

    public AbstractGuiVerticalList() {
    }

    public AbstractGuiVerticalList(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        int width = size.getWidth();
        int height = size.getHeight();
        if (drawShadow) {
            renderer.drawRect(0, 0, width, height, BACKGROUND);
            super.draw(renderer, size, renderInfo);
            renderer.drawRect(0, 0, width, 4, BLACK, BLACK, TRANSPARENT, TRANSPARENT);
            renderer.drawRect(0, height - 4, width, 4, TRANSPARENT, TRANSPARENT, BLACK, BLACK);
        } else {
            super.draw(renderer, size, renderInfo);
        }

        if (drawSlider) {
            ReadableDimension contentSize = listPanel.calcMinSize();
            int contentHeight = contentSize.getHeight() + 10;
            if (contentHeight > height) {
                int sliderX = width / 2 + contentSize.getWidth() / 2 + 3;
                renderer.drawRect(sliderX, 0, 6, height, BLACK); // Draw slider background
                int sliderY = getOffsetY() * height / contentHeight;
                int sliderSize = height * height / contentHeight;
                // Draw slider, with shadows
                renderer.drawRect(sliderX, sliderY, 6, sliderSize, Color.LTGREY); // Slider
                renderer.drawRect(sliderX + 5, sliderY, 1, sliderSize, Color.GREY); // Right shadow
                renderer.drawRect(sliderX, sliderY + sliderSize - 1, 6, 1, Color.GREY); // Bottom shadow
            }
        }
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        position = convert(position);
        if (isOnThis(position)) {
            if (isOnSliderBar(position)) {
                draggingSlider = true;
            }
            lastMousePos = position;
            // We must not return true here
            // because if we did, non of our children were able to process click events at all
        }
        return false;
    }

    @Override
    public boolean mouseDrag(ReadablePoint position, int button, long timeSinceLastCall) {
        position = convert(position);
        if (lastMousePos != null) {
            int dPixel = lastMousePos.getY() - position.getY();
            if (draggingSlider) {
                int contentHeight = listPanel.calcMinSize().getHeight();
                int renderHeight = lastRenderSize.getHeight();
                scrollY(dPixel * (contentHeight + renderHeight) / renderHeight);
            } else {
                scrollY(-dPixel);
            }
            lastMousePos = position;
            // Returning false on purpose, see #mouseClick
        }
        return false;
    }

    @Override
    public boolean mouseRelease(ReadablePoint position, int button) {
        if (lastMousePos != null) {
            lastMousePos = null;
            draggingSlider = false;
            // Returning false on purpose, see #mouseClick
        }
        return false;
    }

    private ReadablePoint convert(ReadablePoint readablePoint) {
        if (getContainer() != null) {
            Point point = new Point(readablePoint);
            getContainer().convertFor(this, point);
            return point;
        }
        return readablePoint;
    }

    private boolean isOnThis(ReadablePoint point) {
        return point.getX() > 0 && point.getY() > 0
                && point.getX() < lastRenderSize.getWidth() && point.getY() < lastRenderSize.getHeight();
    }

    private boolean isOnSliderBar(ReadablePoint point) {
        if (!drawSlider) {
            return false;
        }
        int sliderX = lastRenderSize.getWidth() / 2 + listPanel.calcMinSize().getWidth() / 2 + 3;
        return sliderX <= point.getX() && point.getX() < sliderX + 6;
    }

    private boolean isOnBackground(ReadablePoint point) {
        int width = lastRenderSize.getWidth();
        int listPanelWidth = listPanel.calcMinSize().getWidth();
        return point.getX() < width / 2 - listPanelWidth / 2
                || width / 2 + listPanelWidth / 2 + (drawSlider ? 6 : 0) < point.getX();
    }

    public boolean doesDrawSlider() {
        return drawSlider;
    }

    public T setDrawSlider(boolean drawSlider) {
        this.drawSlider = drawSlider;
        return getThis();
    }

    public boolean doesDrawShadow() {
        return drawShadow;
    }

    public T setDrawShadow(boolean drawShadow) {
        this.drawShadow = drawShadow;
        return getThis();
    }
}
