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
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.function.Clickable;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public abstract class AbstractGuiTimeline<T extends AbstractGuiTimeline<T>> extends AbstractGuiElement<T> implements IGuiTimeline<T>, Clickable {
    protected static final int TEXTURE_WIDTH = 64;
    protected static final int TEXTURE_HEIGHT = 22;

    protected static final int TEXTURE_X = 0;
    protected static final int TEXTURE_Y = 16;

    protected static final int BORDER_LEFT = 4;
    protected static final int BORDER_RIGHT = 4;
    protected static final int BORDER_TOP = 4;
    protected static final int BORDER_BOTTOM = 3;
    protected static final int BODY_WIDTH = TEXTURE_WIDTH - BORDER_LEFT - BORDER_RIGHT;
    protected static final int BODY_HEIGHT = TEXTURE_HEIGHT - BORDER_TOP - BORDER_BOTTOM;

    private OnClick onClick;

    private int length;
    private int cursorPosition;
    private double zoom = 1;
    private int offset;

    private ReadableDimension size;

    public AbstractGuiTimeline() {
    }

    public AbstractGuiTimeline(GuiContainer container) {
        super(container);
    }

    {
        setTooltip(new GuiTooltip(){
            @Override
            public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                setText(getTooltipText(renderInfo));
                super.draw(renderer, size, renderInfo);
            }
        }.setText("00:00"));
    }

    protected String getTooltipText(RenderInfo renderInfo) {
        int ms = getTimeAt(renderInfo.mouseX, renderInfo.mouseY);
        int s = ms / 1000 % 60;
        int m = ms / 1000 / 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        this.size = size;

        int width = size.getWidth();
        int height = size.getHeight();

        // We have to increase the border size as there is one pixel row which is part of the border while drawing
        // but isn't during position calculations due to shadows
        int BORDER_LEFT = GuiTimeline.BORDER_LEFT + 1;
        int BODY_WIDTH = GuiTimeline.BODY_WIDTH - 1;

        renderer.bindTexture(TEXTURE);

        // Left and right borders
        for (int pass = 0; pass < 2; pass++) {
            int x = pass == 0 ? 0 : width - BORDER_RIGHT;
            int textureX = pass == 0 ? TEXTURE_X : TEXTURE_X + TEXTURE_WIDTH - BORDER_RIGHT;
            // Border
            for (int y = BORDER_TOP; y < height - BORDER_BOTTOM; y += BODY_HEIGHT) {
                int segmentHeight = Math.min(BODY_HEIGHT, height - BORDER_BOTTOM - y);
                renderer.drawTexturedRect(x, y, textureX, TEXTURE_Y + BORDER_TOP, BORDER_LEFT, segmentHeight);
            }
            // Top corner
            renderer.drawTexturedRect(x, 0, textureX, TEXTURE_Y, BORDER_LEFT, BORDER_TOP);
            // Bottom corner
            renderer.drawTexturedRect(x, height - BORDER_BOTTOM, textureX, TEXTURE_Y + TEXTURE_HEIGHT - BORDER_BOTTOM,
                    BORDER_LEFT, BORDER_BOTTOM);
        }

        for (int x = BORDER_LEFT; x < width - BORDER_RIGHT; x += BODY_WIDTH) {
            int segmentWidth = Math.min(BODY_WIDTH, width - BORDER_RIGHT - x);
            int textureX = TEXTURE_X + BORDER_LEFT;

            // Content
            for (int y = BORDER_TOP; y < height - BORDER_BOTTOM; y += BODY_HEIGHT) {
                int segmentHeight = Math.min(BODY_HEIGHT, height - BORDER_BOTTOM - y);
                renderer.drawTexturedRect(x, y, textureX, TEXTURE_Y + BORDER_TOP, segmentWidth, segmentHeight);
            }

            // Top border
            renderer.drawTexturedRect(x, 0, textureX, TEXTURE_Y, segmentWidth, BORDER_TOP);
            // Bottom border
            renderer.drawTexturedRect(x, height - BORDER_BOTTOM, textureX, TEXTURE_Y + TEXTURE_HEIGHT - BORDER_BOTTOM,
                    segmentWidth, BORDER_BOTTOM);
        }

        drawTimelineCursor(renderer, size);
    }

    /**
     * Draws the timeline cursor.
     * This is separate from the main draw method so subclasses can repaint the cursor
     * in case it got drawn over by other elements.
     * @param renderer Gui renderer used to draw the cursor
     * @param size Size of the drawable area
     */
    protected void drawTimelineCursor(GuiRenderer renderer, ReadableDimension size) {
        int height = size.getHeight();
        renderer.bindTexture(TEXTURE);

        int visibleLength = (int) (length * zoom);
        int cursor = MathHelper.clamp_int(cursorPosition, offset, offset + visibleLength);
        double positionInVisible = cursor - offset;
        double fractionOfVisible = positionInVisible / visibleLength;
        int cursorX = (int) (BORDER_LEFT + fractionOfVisible * (size.getWidth() - BORDER_LEFT - BORDER_RIGHT));

        // Pin
        renderer.drawTexturedRect(cursorX - 2, BORDER_TOP - 1, 64, 0, 5, 4);
        // Needle
        for (int y = BORDER_TOP - 1; y < height - BORDER_BOTTOM; y += 11) {
            int segmentHeight = Math.min(11, height - BORDER_BOTTOM - y);
            renderer.drawTexturedRect(cursorX - 2, y, 64, 4, 5, segmentHeight);
        }
    }

    /**
     * Returns the time which the mouse is at.
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     * @return The time or -1 if the mouse isn't on the timeline
     */
    protected int getTimeAt(int mouseX, int mouseY) {
        if (size == null) {
            return -1;
        }
        Point mouse = new Point(mouseX, mouseY);
        getContainer().convertFor(this, mouse);
        mouseX = mouse.getX();
        mouseY = mouse.getY();

        if (mouseX < 0 || mouseY < 0
                || mouseX > size.getWidth() || mouseY > size.getHeight()) {
            return -1;
        }

        int width = size.getWidth();
        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
        double segmentLength = length * zoom;
        double segmentTime =  segmentLength * (mouseX - BORDER_LEFT) / bodyWidth;
        return Math.min(Math.max((int) Math.round(offset + segmentTime), 0), length);
    }

    public void onClick(int time) {
        if (onClick != null) {
            onClick.run(time);
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public T setLength(int length) {
        this.length = length;
        return getThis();
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public T setCursorPosition(int position) {
        this.cursorPosition = position;
        return getThis();
    }

    @Override
    public int getCursorPosition() {
        return cursorPosition;
    }

    @Override
    public T setZoom(double zoom) {
        this.zoom = zoom;
        return getThis();
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    @Override
    public T setOffset(int offset) {
        this.offset = offset;
        return getThis();
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public T onClick(OnClick onClick) {
        this.onClick = onClick;
        return getThis();
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        int time = getTimeAt(position.getX(), position.getY());
        if (time != -1) {
            onClick(time);
            return true;
        }
        return false;
    }
}
