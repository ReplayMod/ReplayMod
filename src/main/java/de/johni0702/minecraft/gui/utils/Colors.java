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

package de.johni0702.minecraft.gui.utils;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

public interface Colors extends ReadableColor {
    ReadableColor TRANSPARENT = new Color(0, 0, 0, 0);
    ReadableColor LIGHT_TRANSPARENT = new Color(0, 0, 0, 64);
    ReadableColor HALF_TRANSPARENT = new Color(0, 0, 0, 128);
    ReadableColor DARK_TRANSPARENT = new Color(0, 0, 0, 192);
    ReadableColor LIGHT_GRAY = new Color(192, 192, 192);
    ReadableColor DARK_RED = new Color(170, 0, 0);
}
