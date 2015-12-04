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

import com.google.common.base.Preconditions;
import de.johni0702.minecraft.gui.container.GuiContainer;

import java.util.regex.Pattern;

// TODO: This is suboptimal e.g. if there are trailing zeros, they stay (should be fixed after TextField is done w/o MC)
public abstract class AbstractGuiNumberField<T extends AbstractGuiNumberField<T>>
        extends AbstractGuiTextField<T> implements IGuiNumberField<T> {

    private int precision;
    private volatile Pattern precisionPattern;

    public AbstractGuiNumberField() {
    }

    public AbstractGuiNumberField(GuiContainer container) {
        super(container);
    }

    {
        setValue(0);
    }

    @Override
    public T setText(String text) {
        if (!isTextValid(text)) {
            throw new IllegalArgumentException(text + " is not a valid number!");
        }
        return super.setText(text);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isTextValid(String text) {
        try {
            if (precision == 0) {
                Integer.parseInt(text);
                return true;
            } else {
                Double.parseDouble(text);
                return precisionPattern.matcher(text).matches();
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void onTextChanged(String from) {
        if (isTextValid(getText())) {
            super.onTextChanged(from);
        } else {
            setText(from);
        }
    }

    @Override
    public byte getByte() {
        return Byte.parseByte(getText());
    }

    @Override
    public short getShort() {
        return Short.parseShort(getText());
    }

    @Override
    public int getInteger() {
        return Integer.parseInt(getText());
    }

    @Override
    public long getLong() {
        return Long.parseLong(getText());
    }

    @Override
    public float getFloat() {
        return Float.parseFloat(getText());
    }

    @Override
    public double getDouble() {
        return Double.parseDouble(getText());
    }

    @Override
    public T setValue(int value) {
        setText(Integer.toString(value));
        return getThis();
    }

    @Override
    public T setValue(double value) {
        setText(String.format("%." + precision + "f", value));
        return getThis();
    }

    @Override
    public T setPrecision(int precision) {
        Preconditions.checkArgument(precision >= 0, "precision must not be negative");
        precisionPattern = Pattern.compile(String.format("-?[0-9]*+((\\.[0-9]{0,%d})?)||(\\.)?", precision));
        this.precision = precision;
        return getThis();
    }
}
