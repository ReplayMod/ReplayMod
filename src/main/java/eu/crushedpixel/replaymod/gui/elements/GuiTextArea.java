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

package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.util.MathHelper.clamp_int;

public class GuiTextArea extends Gui implements GuiElement, GuiOutsideClickableElement {

    // Constants
    protected static final int BORDER = 4;
    private static int LINE_SPACING = 2;

    // General
    protected final FontRenderer fontRenderer;
    protected final int guiId;
    public boolean enabled = true;
    public boolean alwaysFocused;

    // Geometry
    public int positionX;
    public int positionY;
    private int width;
    private int height;

    // Content
    private final int maxTextWidth;
    private final int maxTextHeight;
    private final int maxCharCount;

    private String[] text = {""};
    private boolean isFocused;
    private int cursorX;
    private int cursorY;
    private int selectionX;
    private int selectionY;

    // Rendering
    private int currentXOffset;
    private int currentYOffset;
    private int blinkCursorTick;
    public int textColorEnabled = 0xE0E0E0;
    public int textColorDisabled = 0x707070;

    /*
    public GuiTextArea(FontRenderer fontRenderer, int positionX, int positionY, int width, int height,
                       int maxTextWidth, int maxTextHeight) {
        this(0, fontRenderer, positionX, positionY, width, height, maxTextWidth, maxTextHeight, -1);
    }
    */

    public GuiTextArea(FontRenderer fontRenderer, int positionX, int positionY, int width, int height,
                       int maxTextWidth, int maxTextHeight, int maxCharCount) {
        this(0, fontRenderer, positionX, positionY, width, height, maxTextWidth, maxTextHeight, maxCharCount);
    }

    public GuiTextArea(int guiId, FontRenderer fontRenderer, int positionX, int positionY, int width, int height,
                       int maxTextWidth, int maxTextHeight, int maxCharCount) {
        this.guiId = guiId;
        this.fontRenderer = fontRenderer;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.maxTextWidth = maxTextWidth;
        this.maxTextHeight = maxTextHeight;
        this.maxCharCount = maxCharCount;
    }

    public void setText(String[] lines) {
        if (lines.length > maxTextHeight) {
            lines = Arrays.copyOf(lines, maxTextHeight);
        }
        this.text = lines;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > maxTextWidth) {
                lines[i] = lines[i].substring(0, maxTextWidth);
            }
        }
    }

    public String[] getText() {
        return this.text;
    }

    public String getText(int fromX, int fromY, int toX, int toY) {
        StringBuilder sb = new StringBuilder();
        if (fromY == toY) {
            sb.append(text[fromY].substring(fromX, toX));
        } else {
            sb.append(text[fromY].substring(fromX)).append('\n');
            for (int y = fromY + 1; y < toY; y++) {
                sb.append(text[y]).append('\n');
            }
            sb.append(text[toY].substring(0, toX));
        }
        return sb.toString();
    }

    private void deleteText(int fromX, int fromY, int toX, int toY) {
        String[] newText = new String[text.length - (toY - fromY)];
        if (fromY > 0) {
            System.arraycopy(text, 0, newText, 0, fromY);
        }

        newText[fromY] = text[fromY].substring(0, fromX) + text[toY].substring(toX);

        if (toY + 1 < text.length) {
            System.arraycopy(text, toY + 1, newText, fromY + 1, text.length - toY - 1);
        }
        text = newText;
    }

    public int getSelectionFromX() {
        if (cursorY == selectionY) {
            return cursorX > selectionX ? selectionX : cursorX;
        }
        return cursorY > selectionY ? selectionX : cursorX;
    }

    public int getSelectionToX() {
        if (cursorY == selectionY) {
            return cursorX > selectionX ? cursorX : selectionX;
        }
        return cursorY > selectionY ? cursorX : selectionX;
    }

    public int getSelectionFromY() {
        return cursorY > selectionY ? selectionY : cursorY;
    }

    public int getSelectionToY() {
        return cursorY > selectionY ? cursorY : selectionY;
    }

    public String getSelectedText() {
        if (cursorX == selectionX && cursorY == selectionY) {
            return "";
        }
        int fromX = getSelectionFromX();
        int fromY = getSelectionFromY();
        int toX = getSelectionToX();
        int toY = getSelectionToY();
        return getText(fromX, fromY, toX, toY);
    }

    public void deleteSelectedText() {
        if (cursorX == selectionX && cursorY == selectionY) {
            return;
        }
        int fromX = getSelectionFromX();
        int fromY = getSelectionFromY();
        int toX = getSelectionToX();
        int toY = getSelectionToY();
        deleteText(fromX, fromY, toX, toY);
        cursorX = selectionX = fromX;
        cursorY = selectionY = fromY;
        updateCurrentXOffset();
        updateCurrentYOffset();
    }

    private void updateCurrentXOffset() {
        currentXOffset = Math.min(currentXOffset, cursorX);
        String line = text[cursorY].substring(currentXOffset, cursorX);
        int currentWidth = fontRenderer.getStringWidth(line);
        if (currentWidth > width - BORDER * 2) {
            currentXOffset = cursorX - fontRenderer.trimStringToWidth(line, width - BORDER * 2, true).length();
        }
    }

    private void updateCurrentYOffset() {
        currentYOffset = Math.min(currentYOffset, cursorY);
        int lineHeight = fontRenderer.FONT_HEIGHT + LINE_SPACING;
        int contentHeight = height - BORDER * 2;
        int maxLines = contentHeight / lineHeight;
        if (cursorY - currentYOffset >= maxLines) {
            currentYOffset = cursorY - maxLines + 1;
        }
    }

    public String cutSelectedText() {
        String selection = getSelectedText();
        deleteSelectedText();
        return selection;
    }

    public void writeText(String append) {
        for (char c : append.toCharArray()) {
            writeChar(c);
        }
    }

    public void writeChar(char c) {
        if (!ChatAllowedCharacters.isAllowedCharacter(c) && c != '\n') {
            return;
        }

        int totalCharCount = 0;
        for(String line : text) {
            totalCharCount += line.length();
        }

        if(maxCharCount > 0 && totalCharCount-(getSelectedText().length()) >= maxCharCount) {
            return;
        }

        deleteSelectedText();

        if (c == '\n') {
            if (text.length >= maxTextHeight) {
                return;
            }
            String[] newText = new String[text.length + 1];
            if (cursorY > 0) {
                System.arraycopy(text, 0, newText, 0, cursorY);
            }

            newText[cursorY] = text[cursorY].substring(0, cursorX);
            newText[cursorY + 1] = text[cursorY].substring(cursorX);

            if (cursorY + 1 < text.length) {
                System.arraycopy(text, cursorY + 1, newText, cursorY + 2, text.length - cursorY - 1);
            }
            text = newText;
            selectionX = cursorX = 0;
            selectionY = ++cursorY;
            updateCurrentYOffset();
        } else {
            String line = text[cursorY];
            if (line.length() >= maxTextWidth) {
                return;
            }

            line = line.substring(0, cursorX) + c + line.substring(cursorX);
            text[cursorY] = line;
            selectionX = ++cursorX;
        }
        updateCurrentXOffset();
    }

    private void deleteNextChar() {
        String line = text[cursorY];
        if (cursorX < line.length()) {
            line = line.substring(0, cursorX) + line.substring(cursorX + 1);
            text[cursorY] = line;
        } else if (cursorY + 1 < text.length) {
            deleteText(cursorX, cursorY, 0, cursorY + 1);
        }
    }

    private int getNextWordLength() {
        int length = 0;
        String line = text[cursorY];
        boolean inWord = true;
        for (int i = cursorX; i < line.length(); i++) {
            if (inWord) {
                if (line.charAt(i) == ' ') {
                    inWord = false;
                }
            } else {
                if (line.charAt(i) != ' ') {
                    return length;
                }
            }
            length++;
        }
        return length;
    }

    private void deleteNextWord() {
        int worldLength = getNextWordLength();
        if (worldLength == 0) {
            deleteNextChar();
        } else {
            deleteText(cursorX, cursorY, cursorX + worldLength, cursorY);
        }
    }

    private void deletePreviousChar() {
        if (cursorX > 0) {
            String line = text[cursorY];
            line = line.substring(0, cursorX - 1) + line.substring(cursorX);
            selectionX = --cursorX;
            text[cursorY] = line;
        } else if (cursorY > 0) {
            int fromX = text[cursorY - 1].length();
            deleteText(fromX, cursorY - 1, cursorX, cursorY);
            selectionX = cursorX = fromX;
            selectionY = --cursorY;
        }
        updateCurrentXOffset();
    }

    private int getPreviousWordLength() {
        int length = 0;
        String line = text[cursorY];
        boolean inWord = false;
        for (int i = cursorX - 1; i >= 0; i--) {
            if (inWord) {
                if (line.charAt(i) == ' ') {
                    return length;
                }
            } else {
                if (line.charAt(i) != ' ') {
                    inWord = true;
                }
            }
            length++;
        }
        return length;
    }

    private void deletePreviousWord() {
        int worldLength = getPreviousWordLength();
        if (worldLength == 0) {
            deletePreviousChar();
        } else {
            deleteText(cursorX, cursorY, cursorX - worldLength, cursorY);
            selectionX = cursorX -= worldLength;
            updateCurrentXOffset();
        }
    }



    public void setCursorPosition(int x, int y) {
        selectionY = cursorY = clamp_int(y, 0, text.length - 1);
        selectionX = cursorX = clamp_int(x, 0, text[cursorY].length());
        updateCurrentXOffset();
        updateCurrentYOffset();
    }

    public void setFocused(boolean isFocused) {
        isFocused |= alwaysFocused;
        if (isFocused && !this.isFocused) {
            this.blinkCursorTick = 0; // Restart blinking to indicate successful focus
        }

        this.isFocused = isFocused;
    }

    public boolean isFocused() {
        return isFocused || alwaysFocused;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovered) {
        draw(mc, mouseX, mouseY);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        // Draw black rect once pixel smaller than gray rect
        drawRect(positionX, positionY, positionX + width, positionY + height, 0xffa0a0a0);
        drawRect(positionX + 1, positionY + 1, positionX + width - 1, positionY + height - 1, 0xff000000);

        int textColor = this.enabled ? textColorEnabled : textColorDisabled;

        int lineHeight = fontRenderer.FONT_HEIGHT + LINE_SPACING;
        int contentHeight = height - BORDER * 2;
        int maxLines = contentHeight / lineHeight;
        int contentWidth = width - BORDER * 2;

        // Draw lines
        for (int i = 0; i < maxLines && i + currentYOffset < text.length; i++) {
            int lineY = i + currentYOffset;
            String line = text[lineY];
            int leftTrimmed = 0;
            if (lineY == cursorY) {
                line = line.substring(currentXOffset);
                leftTrimmed = currentXOffset;
            }
            line = fontRenderer.trimStringToWidth(line, contentWidth);

            // Draw line
            int posY = positionY + BORDER + i * lineHeight;
            int lineEnd = fontRenderer.drawStringWithShadow(line, positionX + BORDER, posY, textColor);

            // Draw selection
            int fromX = getSelectionFromX();
            int fromY = getSelectionFromY();
            int toX = getSelectionToX();
            int toY = getSelectionToY();
            if (lineY > fromY && lineY < toY) { // Whole line selected
                invertColors(lineEnd, posY - 1 + lineHeight, positionX + BORDER, posY - 1);
            } else if (lineY == fromY && lineY == toY) { // Part of line selected
                String leftStr = line.substring(0, clamp_int(fromX - leftTrimmed, 0, line.length()));
                String rightStr = line.substring(clamp_int(toX - leftTrimmed, 0, line.length()));
                int left = positionX + BORDER + fontRenderer.getStringWidth(leftStr);
                int right = lineEnd - fontRenderer.getStringWidth(rightStr) - 1;
                invertColors(right, posY - 1 + lineHeight, left, posY - 1);
            } else if (lineY == fromY) { // End of line selected
                String rightStr = line.substring(clamp_int(fromX - leftTrimmed, 0, line.length()));
                invertColors(lineEnd, posY - 1 + lineHeight, lineEnd - fontRenderer.getStringWidth(rightStr), posY - 1);
            } else if (lineY == toY) { // Beginning of line selected
                String leftStr = line.substring(0, clamp_int(toX - leftTrimmed, 0, line.length()));
                int right = positionX + BORDER + fontRenderer.getStringWidth(leftStr);
                invertColors(right, posY - 1 + lineHeight, positionX + BORDER, posY - 1);
            }

            // Draw cursor
            if (lineY == cursorY && blinkCursorTick / 6 % 2 == 0 && isFocused) {
                String beforeCursor = line.substring(0, cursorX - leftTrimmed);
                int posX = positionX + BORDER + fontRenderer.getStringWidth(beforeCursor);
                if (cursorX == text[lineY].length()) {
                    fontRenderer.drawStringWithShadow("_", posX, posY, 0xfff0f0f0);
                } else {
                    drawRect(posX, posY - 1, posX + 1, posY + 1 + fontRenderer.FONT_HEIGHT, 0xfff0f0f0);
                }
            }
        }
    }

    private void invertColors(int right, int bottom, int left, int top) {
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
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {

    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= positionX
                && mouseY >= positionY
                && mouseX < positionX + width
                && mouseY < positionY + height;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        boolean hovering = isHovering(mouseX, mouseY);

        if (hovering && isFocused() && button == 0) {
            mouseX -= positionX + BORDER;
            mouseY -= positionY + BORDER;
            int textY = clamp_int(mouseY / (fontRenderer.FONT_HEIGHT + LINE_SPACING) + currentYOffset, 0, text.length - 1);
            if (cursorY != textY) {
                currentXOffset = 0;
            }
            String line = text[textY].substring(currentXOffset);
            int textX = fontRenderer.trimStringToWidth(line, mouseX).length() + currentXOffset;
            setCursorPosition(textX, textY);
        }

        setFocused(hovering);
        return hovering;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {
        if (!this.isFocused) {
            return;
        }

        if (GuiScreen.isCtrlKeyDown()) {
            switch (keyCode) {
                case Keyboard.KEY_A: // Select all
                    cursorX = cursorY = 0;
                    selectionY = text.length - 1;
                    selectionX = text[selectionY].length();
                    updateCurrentXOffset();
                    updateCurrentYOffset();
                    return;
                case Keyboard.KEY_C: // Copy
                    GuiScreen.setClipboardString(getSelectedText());
                    return;
                case Keyboard.KEY_V: // Paste
                    if (enabled) {
                        writeText(GuiScreen.getClipboardString());
                    }
                    return;
                case Keyboard.KEY_X: // Cut
                    if (enabled) {
                        GuiScreen.setClipboardString(cutSelectedText());
                    }
                    return;
            }
        }

        boolean words = GuiScreen.isCtrlKeyDown();
        boolean select = GuiScreen.isShiftKeyDown();
        switch (keyCode) {
            case Keyboard.KEY_HOME:
                cursorX = 0;
                break;
            case Keyboard.KEY_END:
                cursorX = text[cursorY].length();
                break;
            case Keyboard.KEY_LEFT:
                if (cursorX == 0) {
                    if (cursorY > 0) {
                        cursorY--;
                        cursorX = text[cursorY].length();
                    }
                } else {
                    if (words) {
                        cursorX -= getPreviousWordLength();
                    } else {
                        cursorX--;
                    }
                }
                break;
            case Keyboard.KEY_RIGHT:
                if (cursorX == text[cursorY].length()) {
                    if (cursorY < text.length - 1) {
                        cursorY++;
                        cursorX = 0;
                    }
                } else {
                    if (words) {
                        cursorX += getNextWordLength();
                    } else {
                        cursorX++;
                    }
                }
                break;
            case Keyboard.KEY_UP:
                if (cursorY > 0) {
                    cursorY--;
                    cursorX = Math.min(cursorX, text[cursorY].length());
                }
                break;
            case Keyboard.KEY_DOWN:
                if (cursorY + 1 < text.length) {
                    cursorY++;
                    cursorX = Math.min(cursorX, text[cursorY].length());
                }
                break;
            case Keyboard.KEY_BACK:
                if (enabled) {
                    if (getSelectedText().length() > 0) {
                        deleteSelectedText();
                    } else if (words) {
                        deletePreviousWord();
                    } else {
                        deletePreviousChar();
                    }
                }
                return;
            case Keyboard.KEY_DELETE:
                if (enabled) {
                    if (getSelectedText().length() > 0) {
                        deleteSelectedText();
                    } else if (words) {
                        deleteNextWord();
                    } else {
                        deleteNextChar();
                    }
                }
                return;
            default:
                if (enabled) {
                    if (key == '\r') {
                        key = '\n';
                    }
                    writeChar(key);
                }
                return;
        }

        updateCurrentXOffset();
        updateCurrentYOffset();

        if (!select) {
            selectionX = cursorX;
            selectionY = cursorY;
        }
    }

    @Override
    public void tick(Minecraft mc) {
        blinkCursorTick++;
    }

    @Override
    public void setElementEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setWidth(int width) {
        this.width = width;
        updateCurrentXOffset();
    }

    @Override
    public int xPos() {
        return positionX;
    }

    @Override
    public int yPos() {
        return positionY;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void xPos(int x) {
        positionX = x;
    }

    @Override
    public void yPos(int y) {
        positionY = y;
    }

    @Override
    public void width(int width) {
        this.width = width;
    }

    @Override
    public void height(int height) {
        this.height = height;
    }
}
