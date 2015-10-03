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

import de.johni0702.minecraft.gui.element.GuiElement;

public interface IGuiTimeline<T extends IGuiTimeline<T>> extends GuiElement<T> {

    /**
     * Set the total length of the timeline.
     * @param length length in milliseconds, must be > 0
     * @return {@code this}, for chaining
     */
    T setLength(int length);

    /**
     * Returns the total length of the timeline.
     * @return The total length in millisconds
     */
    int getLength();

    /**
     * Set the current position of the cursor. Should be between 0 and {@link #getLength()}.
     * @param position Position of the cursor in milliseconds
     * @return {@code this}, for chaining
     */
    T setCursorPosition(int position);

    /**
     * Returns the current position of the cursor. Should be between 0 and {@link #getLength()}.
     * @return cursor position in milliseconds
     */
    int getCursorPosition();

    /**
     * Set the zoom of this timeline. 1/10 allows the user to see 1/10 of the total length.
     * @param zoom The zoom factor. Must be between 1 (inclusive) and 0 (exclusive)
     * @return {@code this}, for chaining
     */
    T setZoom(double zoom);

    /**
     * Returns the zoom of this timeline. 1/10 allows the user to see 1/10 of the total length.
     * @return The zoom factor. Must be between 1 (inclusive) and 0 (exclusive)
     */
    double getZoom();

    /**
     * Set the position of the timeline which should be shown.
     * The left side of the timeline will start at this offset.
     * @param offset The offset in milliseconds
     * @return {@code this}, for chaining
     */
    T setOffset(int offset);

    /**
     * Returns the position of the timeline which should be shown.
     * The left side of the timeline will start at this offset.
     * @return The offset in milliseconds
     */
    int getOffset();

    T onClick(OnClick onClick);

    interface OnClick {
        void run(int time);
    }
}
