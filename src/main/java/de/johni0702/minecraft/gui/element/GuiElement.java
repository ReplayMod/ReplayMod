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
import net.minecraft.client.Minecraft;
import org.lwjgl.util.ReadableDimension;

public interface GuiElement<T extends GuiElement<T>> {

    Minecraft getMinecraft();

    GuiContainer getContainer();
    T setContainer(GuiContainer container);

    void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo);

    ReadableDimension getMinSize();
    ReadableDimension getMaxSize();

    T setMaxSize(ReadableDimension maxSize);

    boolean isEnabled();
    T setEnabled(boolean enabled);
    T setEnabled();
    T setDisabled();

    GuiElement getTooltip(RenderInfo renderInfo);
    T setTooltip(GuiElement tooltip);

    /**
     * Returns the layer this element takes part in.
     * The standard layer is layer 0. Event handlers will be called for this layer.
     * @return The layer of this element
     */
    int getLayer();

}
