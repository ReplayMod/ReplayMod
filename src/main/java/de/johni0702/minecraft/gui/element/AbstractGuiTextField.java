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
import com.google.common.base.Strings;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.function.Clickable;
import de.johni0702.minecraft.gui.function.Focusable;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.utils.Consumer;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.*;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.client.renderer.GlStateManager.disableColorLogic;
import static net.minecraft.client.renderer.GlStateManager.enableTexture2D;
import static net.minecraft.util.MathHelper.clamp_int;

public abstract class AbstractGuiTextField<T extends AbstractGuiTextField<T>>
        extends AbstractGuiElement<T> implements Clickable, Tickable, Typeable, IGuiTextField<T> {
    private static final ReadableColor BORDER_COLOR = new Color(160, 160, 160);
    private static final ReadableColor CURSOR_COLOR = new Color(240, 240, 240);
    private static final int BORDER = 4;

    // Focus
    @Getter
    private boolean focused;
    @Getter
    private Focusable next, previous;

    // Content
    @Getter
    private int maxLength = 32;

    @Getter
    @NonNull
    private String text = "";

    private int cursorPos;
    private int selectionPos;

    @Getter
    private String hint;

    // Rendering
    private int currentOffset;
    private int blinkCursorTick;
    public ReadableColor textColorEnabled = new Color(224, 224, 224);
    public ReadableColor textColorDisabled = new Color(112, 112, 112);
    private ReadableDimension size = new Dimension(0, 0); // Size of last render

    private Consumer<String> textChanged;
    private Runnable onEnter;

    public AbstractGuiTextField() {
    }

    public AbstractGuiTextField(GuiContainer container) {
        super(container);
    }

    @Override
    public T setText(String text) {
        if (text.length() > maxLength) {
            this.text = text.substring(0, maxLength);
        } else {
            this.text = text;
        }
        selectionPos = cursorPos = text.length();
        return getThis();
    }

    @Override
    public T setI18nText(@NonNull String text, @NonNull Object... args) {
        return setText(I18n.format(text, args));
    }

    @Override
    public T setMaxLength(int maxLength) {
        Preconditions.checkArgument(maxLength >= 0, "maxLength must not be negative");
        this.maxLength = maxLength;
        if (text.length() > maxLength) {
            setText(text);
        }
        return getThis();
    }

    @Override
    public String deleteText(int from, int to) {
        Preconditions.checkArgument(from <= to, "from must not be greater than to");
        Preconditions.checkArgument(from >= 0, "from must be greater than zero");
        Preconditions.checkArgument(to < text.length(), "to must be less than test.length()");

        String deleted = text.substring(from, to + 1);
        text = text.substring(0, from) + text.substring(to + 1);
        return deleted;
    }

    @Override
    public int getSelectionFrom() {
        return cursorPos > selectionPos ? selectionPos : cursorPos;
    }

    @Override
    public int getSelectionTo() {
        return cursorPos > selectionPos ? cursorPos : selectionPos;
    }

    @Override
    public String getSelectedText() {
        return text.substring(getSelectionFrom(), getSelectionTo());
    }

    @Override
    public String deleteSelectedText() {
        if (cursorPos == selectionPos) {
            return ""; // Nothing selected
        }
        int from = getSelectionFrom();
        String deleted = deleteText(from, getSelectionTo() - 1);
        cursorPos = selectionPos = from;
        updateCurrentOffset();
        return deleted;
    }

    /**
     * Update current text offset to make sure the cursor is always visible.
     */
    private void updateCurrentOffset() {
        currentOffset = Math.min(currentOffset, cursorPos);
        String line = text.substring(currentOffset, cursorPos);
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        int currentWidth = fontRenderer.getStringWidth(line);
        if (currentWidth > size.getWidth() - 2*BORDER) {
            currentOffset = cursorPos - fontRenderer.trimStringToWidth(line, size.getWidth() - 2*BORDER, true).length();
        }
    }

    @Override
    public T writeText(String append) {
        for (char c : append.toCharArray()) {
            writeChar(c);
        }
        return getThis();
    }

    @Override
    public T writeChar(char c) {
        if (!ChatAllowedCharacters.isAllowedCharacter(c)) {
            return getThis();
        }

        deleteSelectedText();

        if (text.length() >= maxLength) {
            return getThis();
        }

        text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
        selectionPos = ++cursorPos;

        updateCurrentOffset();
        return getThis();
    }

    @Override
    public T deleteNextChar() {
        if (cursorPos < text.length()) {
            text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
        }
        selectionPos = cursorPos;
        return getThis();
    }

    /**
     * Return the amount of characters to the next word (excluding).
     * If this is the last word in the line, return the amount of characters remaining to till the end.
     * Everything except the Space character is considered part of a word.
     * @return Length in characters
     */
    protected int getNextWordLength() {
        int length = 0;
        boolean inWord = true;
        for (int i = cursorPos; i < text.length(); i++) {
            if (inWord) {
                if (text.charAt(i) == ' ') {
                    inWord = false;
                }
            } else {
                if (text.charAt(i) != ' ') {
                    return length;
                }
            }
            length++;
        }
        return length;
    }

    @Override
    public String deleteNextWord() {
        int worldLength = getNextWordLength();
        if (worldLength > 0) {
            return deleteText(cursorPos, cursorPos + worldLength);
        }
        return "";
    }

    @Override
    public T deletePreviousChar() {
        if (cursorPos > 0) {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            selectionPos = --cursorPos;
            updateCurrentOffset();
        }
        return getThis();
    }

    /**
     * Return the amount of characters to the previous word (including).
     * If this is the first word in the line, return the amount of characters till the start.
     * Everything except the Space character is considered part of a word.
     * @return Length in characters
     */
    protected int getPreviousWordLength() {
        int length = 0;
        boolean inWord = false;
        for (int i = cursorPos - 1; i >= 0; i--) {
            if (inWord) {
                if (text.charAt(i) == ' ') {
                    return length;
                }
            } else {
                if (text.charAt(i) != ' ') {
                    inWord = true;
                }
            }
            length++;
        }
        return length;
    }

    @Override
    public String deletePreviousWord() {
        int worldLength = getPreviousWordLength();
        String deleted = "";
        if (worldLength > 0) {
            deleted = deleteText(cursorPos - worldLength, cursorPos);
            selectionPos = cursorPos -= worldLength;
            updateCurrentOffset();
        }
        return deleted;
    }

    @Override
    public T setCursorPosition(int pos) {
        Preconditions.checkArgument(pos >= 0 && pos <= text.length());
        selectionPos = cursorPos = pos;
        updateCurrentOffset();
        return getThis();
    }

    /**
     * Inverts all colors on the screen.
     * @param guiRenderer The GUI Renderer
     * @param right Right border of the inverted rectangle
     * @param bottom Bottom border of the inverted rectangle
     * @param left Left border of the inverted rectangle
     * @param top Top border of the inverted rectangle
     */
    private void invertColors(GuiRenderer guiRenderer, int right, int bottom, int left, int top) {
        int x = guiRenderer.getOpenGlOffset().getX();
        int y = guiRenderer.getOpenGlOffset().getY();
        right+=x;
        left+=x;
        bottom+=y;
        top+=y;

        color(0, 0, 255, 255);
        disableTexture2D();
        enableColorLogic();
        colorLogicOp(GL11.GL_OR_REVERSE);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.startDrawingQuads();
        renderer.addVertex(right, top, 0);
        renderer.addVertex(left, top, 0);
        renderer.addVertex(left, bottom, 0);
        renderer.addVertex(right, bottom, 0);
        tessellator.draw();

        disableColorLogic();
        enableTexture2D();
    }

    @Override
    protected ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        if (getContainer() != null) {
            getContainer().convertFor(this, (Point) (position = new Point(position)));
        }
        boolean hovering = isMouseHovering(position);

        if (hovering && isFocused() && button == 0) {
            int mouseX = position.getX() - BORDER;
            FontRenderer fontRenderer = getMinecraft().fontRendererObj;
            String text = this.text.substring(currentOffset);
            int textX = fontRenderer.trimStringToWidth(text, mouseX).length() + currentOffset;
            setCursorPosition(textX);
        }

        setFocused(hovering);
        return hovering;
    }

    protected boolean isMouseHovering(ReadablePoint pos) {
        return pos.getX() > 0 && pos.getY() > 0
                && pos.getX() < size.getWidth() && pos.getY() < size.getHeight();
    }

    @Override
    public T setFocused(boolean isFocused) {
        if (isFocused && !this.focused) {
            this.blinkCursorTick = 0; // Restart blinking to indicate successful focus
        }
        this.focused = isFocused;
        return getThis();
    }

    @Override
    public T setNext(Focusable next) {
        this.next = next;
        return getThis();
    }

    @Override
    public T setPrevious(Focusable previous) {
        this.previous = previous;
        return getThis();
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        this.size = size;
        int width = size.getWidth(), height = size.getHeight();
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        int posY = height / 2 - fontRenderer.FONT_HEIGHT / 2;

        // Draw black rect once pixel smaller than gray rect
        renderer.drawRect(0, 0, width, height, BORDER_COLOR);
        renderer.drawRect(1, 1, width - 2, height - 2, ReadableColor.BLACK);

        if (text.isEmpty() && !isFocused() && !Strings.isNullOrEmpty(hint)) {
            // Draw hint
            String text = fontRenderer.trimStringToWidth(hint, width - 2*BORDER);
            renderer.drawString(BORDER, posY, textColorDisabled, text);
        } else {
            // Draw text
            String renderText = text.substring(currentOffset);
            renderText = fontRenderer.trimStringToWidth(renderText, width - 2*BORDER);
            ReadableColor color = isEnabled() ? textColorEnabled : textColorDisabled;
            int lineEnd = renderer.drawString(BORDER, height / 2 - fontRenderer.FONT_HEIGHT / 2, color, renderText);

            // Draw selection
            int from = getSelectionFrom();
            int to = getSelectionTo();
            String leftStr = text.substring(0, clamp_int(from - currentOffset, 0, text.length()));
            String rightStr = text.substring(clamp_int(to - currentOffset, 0, text.length()));
            int left = BORDER + fontRenderer.getStringWidth(leftStr);
            int right = lineEnd - fontRenderer.getStringWidth(rightStr) - 1;
            invertColors(renderer, right, height - 2, left, 2);

            // Draw cursor
            if (blinkCursorTick / 6 % 2 == 0 && focused) {
                String beforeCursor = text.substring(0, cursorPos - currentOffset);
                int posX = BORDER + fontRenderer.getStringWidth(beforeCursor);
                if (cursorPos == text.length()) {
                    renderer.drawString(posX, posY, CURSOR_COLOR, "_", true);
                } else {
                    renderer.drawRect(posX, posY - 1, 1, 1 + fontRenderer.FONT_HEIGHT, CURSOR_COLOR);
                }
            }
        }
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (!this.focused) {
            return false;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            Focusable other = shiftDown ? previous : next;
            if (other != null) {
                setFocused(false);
                other.setFocused(true);
            }
            return true;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            onEnter();
            return true;
        }

        String textBefore = text;
        try {
            if (GuiScreen.isCtrlKeyDown()) {
                switch (keyCode) {
                    case Keyboard.KEY_A: // Select all
                        cursorPos = 0;
                        selectionPos = text.length();
                        updateCurrentOffset();
                        return true;
                    case Keyboard.KEY_C: // Copy
                        GuiScreen.setClipboardString(getSelectedText());
                        return true;
                    case Keyboard.KEY_V: // Paste
                        if (isEnabled()) {
                            writeText(GuiScreen.getClipboardString());
                        }
                        return true;
                    case Keyboard.KEY_X: // Cut
                        if (isEnabled()) {
                            GuiScreen.setClipboardString(deleteSelectedText());
                        }
                        return true;
                }
            }

            boolean words = GuiScreen.isCtrlKeyDown();
            boolean select = GuiScreen.isShiftKeyDown();
            switch (keyCode) {
                case Keyboard.KEY_HOME:
                    cursorPos = 0;
                    break;
                case Keyboard.KEY_END:
                    cursorPos = text.length();
                    break;
                case Keyboard.KEY_LEFT:
                    if (cursorPos != 0) {
                        if (words) {
                            cursorPos -= getPreviousWordLength();
                        } else {
                            cursorPos--;
                        }
                    }
                    break;
                case Keyboard.KEY_RIGHT:
                    if (cursorPos != text.length()) {
                        if (words) {
                            cursorPos += getNextWordLength();
                        } else {
                            cursorPos++;
                        }
                    }
                    break;
                case Keyboard.KEY_BACK:
                    if (isEnabled()) {
                        if (getSelectedText().length() > 0) {
                            deleteSelectedText();
                        } else if (words) {
                            deletePreviousWord();
                        } else {
                            deletePreviousChar();
                        }
                    }
                    return true;
                case Keyboard.KEY_DELETE:
                    if (isEnabled()) {
                        if (getSelectedText().length() > 0) {
                            deleteSelectedText();
                        } else if (words) {
                            deleteNextWord();
                        } else {
                            deleteNextChar();
                        }
                    }
                    return true;
                default:
                    if (isEnabled()) {
                        if (keyChar == '\r') {
                            keyChar = '\n';
                        }
                        writeChar(keyChar);
                    }
                    return true;
            }

            updateCurrentOffset();

            if (!select) {
                selectionPos = cursorPos;
            }
            return true;
        } finally {
            if (!textBefore.equals(text)) {
                onTextChanged(textBefore);
            }
        }
    }

    @Override
    public void tick() {
        blinkCursorTick++;
    }

    /**
     * Called when the user presses the Enter/Return key while this text field is focused.
     */
    protected void onEnter() {
        if (onEnter != null) {
            onEnter.run();
        }
    }

    /**
     * Called when the text has changed due to user input.
     */
    protected void onTextChanged(String from) {
        if (textChanged != null) {
            textChanged.consume(from);
        }
    }

    @Override
    public T onEnter(Runnable onEnter) {
        this.onEnter = onEnter;
        return getThis();
    }

    @Override
    public T onTextChanged(Consumer<String> textChanged) {
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
    public ReadableColor getTextColor() {
        return textColorEnabled;
    }

    @Override
    public T setTextColor(ReadableColor textColor) {
        this.textColorEnabled = textColor;
        return getThis();
    }

    @Override
    public T setTextColorDisabled(ReadableColor textColorDisabled) {
        this.textColorDisabled = textColorDisabled;
        return getThis();
    }
}
