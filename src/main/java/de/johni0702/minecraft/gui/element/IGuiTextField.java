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

import de.johni0702.minecraft.gui.function.Focusable;
import de.johni0702.minecraft.gui.utils.Consumer;
import lombok.NonNull;
import org.lwjgl.util.ReadableColor;

public interface IGuiTextField<T extends IGuiTextField<T>> extends GuiElement<T>, Focusable<T> {
    /**
     * Set the text to the specified string.
     * If the string is longer than {@link #getMaxLength()} it is truncated from the end.
     * This method positions the cursor at the end of the text and removes any selections.
     * @param text The new text
     * @return {@code this} for chaining
     */
    @NonNull T setText(@NonNull String text);

    /**
     * Set the text to the specified string.
     * If the string is longer than {@link #getMaxLength()} it is truncated from the end.
     * This method positions the cursor at the end of the text and removes any selections.
     * @param text The language key for the new text
     * @param args The arguments used in translating the language key
     * @return {@code this} for chaining
     */
    @NonNull T setI18nText(@NonNull String text, @NonNull Object... args);

    /**
     * Return the whole text in this text field.
     * @return The text, may be empty
     */
    @NonNull String getText();

    /**
     * Return the maximum allowed length of the text in this text field.
     * @return Maximum number of characters
     */
    int getMaxLength();

    /**
     * Set the maximum allowed length of the text in this text field.
     * If the current test is longer than the new limit, it is truncated from the end (the cursor and selection
     * are reset in that process, see {@link #setText(String)}).
     * @param maxLength Maximum number of characters
     * @return {@code this} for chaining
     * @throws IllegalArgumentException When {@code maxLength} is negative
     */
    T setMaxLength(int maxLength);

    /**
     * Deletes the text between {@code from} and {@code to} (inclusive).
     * @param from Index at which to start
     * @param to Index to which to delete
     * @return The deleted text
     * @throws IllegalArgumentException If {@code from} is greater than {@code to} or either is out of bounds
     */
    @NonNull String deleteText(int from, int to);

    /**
     * Return the index at which the selection starts (inclusive)
     * @return Index of first character
     */
    int getSelectionFrom();

    /**
     * Return the index at which the selection ends (exclusive)
     * @return Index after the last character
     */
    int getSelectionTo();

    /**
     * Return the selected text.
     * @return The selected text
     */
    @NonNull String getSelectedText();

    /**
     * Delete the selected text. Positions the cursor at the beginning of the selection and clears the selection.
     * @return The deleted text
     */
    @NonNull String deleteSelectedText();

    /**
     * Appends the specified string to this text field character by character.
     * Excess characters are ignored.
     * @param append String to append
     * @return {@code this} for chaining
     * @see #writeChar(char)
     */
    @NonNull T writeText(@NonNull String append);

    /**
     * Appends the specified character to this text field replacing the current selection (if any).
     * This does nothing if the maximum character limit is reached.
     * @param c Character to append
     * @return {@code this} for chaining
     */
    @NonNull T writeChar(char c);

    /**
     * Delete the nex character (if any).
     * Clears the selection.
     * @return {@code this} for chaining
     */
    T deleteNextChar();

    /**
     * Delete everything from the cursor (inclusive) to the beginning of the next word (exclusive).
     * If there are no more words, delete everything until the end of the line.
     * @return The deleted text
     */
    String deleteNextWord();

    /**
     * Delete the previous character (if any).
     * @return {@code this} for chaining
     */
    @NonNull T deletePreviousChar();

    /**
     * Delete everything from cursor to the first character of the previous word (or the start of the line).
     * @return The deleted text
     */
    @NonNull String deletePreviousWord();

    /**
     * Set the cursor position.
     * @param pos Position of the cursor
     * @return {@code this} for chaining
     * @throws IllegalArgumentException If {@code pos} &lt 0 or pos &gt length
     */
    @NonNull T setCursorPosition(int pos);

    T onEnter(Runnable onEnter);
    T onTextChanged(Consumer<String> textChanged);

    String getHint();
    T setHint(String hint);
    T setI18nHint(String hint, Object... args);

    ReadableColor getTextColor();
    T setTextColor(ReadableColor textColor);
    T setTextColorDisabled(ReadableColor textColorDisabled);
}
