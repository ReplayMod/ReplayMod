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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.GuiElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class GridLayout implements Layout {
    private static final Data DEFAULT_DATA = new Data();

    @Accessors(chain = true)
    @Getter
    @Setter
    private int columns;

    @Accessors(chain = true)
    @Getter
    @Setter
    private int spacingX, spacingY;

    @Override
    public Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layOut(GuiContainer<?> container, ReadableDimension size) {
        Preconditions.checkState(columns != 0, "Columns may not be 0.");
        int elements = container.getElements().size();
        int rows = (elements - 1 + columns) / columns;
        if (rows < 1) {
            return Collections.emptyMap();
        }
        int cellWidth = (size.getWidth() + spacingX) / columns - spacingX;
        int cellHeight = (size.getHeight() + spacingY) / rows - spacingY;
        Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> map = Maps.newHashMap();
        Iterator<Map.Entry<GuiElement, LayoutData>> iter = container.getElements().entrySet().iterator();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (!iter.hasNext()) {
                    return map;
                }
                int x = j * (cellWidth + spacingX);
                int y = i * (cellHeight + spacingY);

                Map.Entry<GuiElement, LayoutData> entry = iter.next();
                GuiElement element  = entry.getKey();
                Data data = entry.getValue() instanceof Data ? (Data) entry.getValue() : DEFAULT_DATA;
                Dimension elementSize = new Dimension(element.getMinSize());
                ReadableDimension elementMaxSize = element.getMaxSize();
                elementSize.setWidth(Math.min(cellWidth, elementMaxSize.getWidth()));
                elementSize.setHeight(Math.min(cellHeight, elementMaxSize.getHeight()));

                int remainingWidth = cellWidth - elementSize.getWidth();
                int remainingHeight = cellHeight - elementSize.getHeight();
                x += (int) (data.alignmentX * remainingWidth);
                y += (int) (data.alignmentY * remainingHeight);
                map.put(element, Pair.<ReadablePoint, ReadableDimension>of(new Point(x, y), elementSize));
            }
        }
        return map;
    }

    @Override
    public ReadableDimension calcMinSize(GuiContainer<?> container) {
        Preconditions.checkState(columns != 0, "Columns may not be 0.");
        int maxWidth = 0, maxHeight = 0;
        int elements = 0;
        for (Map.Entry<GuiElement, LayoutData> entry : container.getElements().entrySet()) {
            GuiElement element = entry.getKey();
            ReadableDimension minSize = element.getMinSize();
            int width = minSize.getWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }
            int height = minSize.getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
            elements++;
        }
        int rows = (elements - 1 + columns) / columns;
        int totalWidth = maxWidth * columns;
        int totalHeight = maxHeight * rows;
        if (elements > 0) {
            totalWidth+=spacingX * (columns - 1);
        }
        if (elements > columns) {
            totalHeight+=spacingY * (rows - 1);
        }
        return new Dimension(totalWidth, totalHeight);
    }

    @lombok.Data
    @AllArgsConstructor
    public static class Data implements LayoutData {
        private double alignmentX, alignmentY;

        public Data() {
            this(0, 0);
        }
    }
}
