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

import com.google.common.base.Strings;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.function.Clickable;
import de.johni0702.minecraft.gui.function.Focusable;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.function.Typeable;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public abstract class AbstractGuiTextField<T extends AbstractGuiTextField<T>>
        extends AbstractGuiElement<T> implements Clickable, Tickable, Typeable, IGuiTextField<T> {
    private final net.minecraft.client.gui.GuiTextField wrapped;

    private Runnable textChanged;
    private Runnable onEnter;

    private Focusable next, previous;

    @Getter
    private String hint;

    @Getter
    private ReadableColor textColor;

    public AbstractGuiTextField() {
        this.wrapped = new net.minecraft.client.gui.GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, 0, 0);
    }

    public AbstractGuiTextField(GuiContainer container) {
        super(container);
        this.wrapped = new net.minecraft.client.gui.GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, 0, 0);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        ReadablePoint position = renderer.getOpenGlOffset();
        wrapped.xPosition = position.getX() + 1;
        wrapped.yPosition = position.getY() + 1;
        wrapped.width = size.getWidth() - 2;
        wrapped.height = size.getHeight() - 2;

        if (wrapped.text.isEmpty() && !isFocused() && !Strings.isNullOrEmpty(hint)) {
            wrapped.setEnabled(false);
            wrapped.setDisabledTextColour(0xff404040);
            wrapped.text = hint;
            wrapped.drawTextBox();
            wrapped.text = "";
            wrapped.setDisabledTextColour(0xff707070);
            wrapped.setEnabled(isEnabled());
        } else {
            wrapped.drawTextBox();
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        return new Dimension(0, fontRenderer.FONT_HEIGHT);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        wrapped.mouseClicked(position.getX(), position.getY(), button);
        return false;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (!isFocused()) {
            return false;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            onEnter();
            return true;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            Focusable other = shiftDown ? previous : next;
            if (other != null) {
                setFocused(false);
                other.setFocused(true);
            }
            return true;
        }

        String before = wrapped.getText();
        wrapped.textboxKeyTyped(keyChar, keyCode);
        String after = wrapped.getText();
        if (!before.equals(after)) {
            if (textChanged != null) {
                textChanged.run();
            }
        }
        return true;
    }

    @Override
    public T setText(String text) {
        if (text.length() > wrapped.getMaxStringLength()) {
            wrapped.text = text.substring(0, wrapped.getMaxStringLength());
        } else {
            wrapped.text = text;
        }
        return getThis();
    }

    @Override
    public T setI18nText(String text, Object... args) {
        return setText(I18n.format(text, args));
    }

    @Override
    public String getText() {
        return wrapped.text;
    }

    @Override
    public boolean isFocused() {
        return wrapped.isFocused();
    }

    @Override
    public T setFocused(boolean focused) {
        wrapped.setFocused(focused);
        return getThis();
    }

    @Override
    public Focusable getNext() {
        return next;
    }

    @Override
    public T setNext(Focusable next) {
        if (this.next == next) return getThis();
        if (this.next != null) {
            this.next.setPrevious(null);
        }
        this.next = next;
        if (this.next != null) {
            this.next.setPrevious(this);
        }
        return getThis();
    }

    @Override
    public Focusable getPrevious() {
        return previous;
    }

    @Override
    public T setPrevious(Focusable previous) {
        if (this.previous == previous) return getThis();
        if (this.previous != null) {
            this.previous.setNext(null);
        }
        this.previous = previous;
        if (this.previous != null) {
            this.previous.setNext(this);
        }
        return getThis();
    }

    @Override
    public int getMaxLength() {
        return wrapped.getMaxStringLength();
    }

    @Override
    public T setMaxLength(int maxLength) {
        wrapped.setMaxStringLength(maxLength);
        return getThis();
    }

    @Override
    public void tick() {
        wrapped.updateCursorCounter();
    }

    protected void onEnter() {
        if (onEnter != null) {
            onEnter.run();
        }
    }

    @Override
    public T onEnter(Runnable onEnter) {
        this.onEnter = onEnter;
        return getThis();
    }

    @Override
    public T onTextChanged(Runnable textChanged) {
        this.textChanged = textChanged;
        return getThis();
    }

    @Override
    public T setHint(String hint) {
        this.hint = hint;
        return getThis();
    }

    @Override
    public T setI18nHint(String hint, Object... args) {
        return setHint(I18n.format(hint));
    }

    @Override
    public T setTextColor(ReadableColor textColor) {
        this.textColor = textColor;
        wrapped.setTextColor(textColor.getAlpha() << 24 | textColor.getRed() << 16 | textColor.getGreen() << 8 | textColor.getBlue());
        return getThis();
    }
}
