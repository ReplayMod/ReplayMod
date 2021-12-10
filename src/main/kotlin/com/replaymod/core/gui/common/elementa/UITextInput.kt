/**
 * Based on Elementa's UITextInput but with more stuff exposed and certain changes so we can get it to behave just
 * like we need it to:
 * - No changes except it is using our AbstractTextInput (which has changes)
 *
 * MIT License
 *
 * Copyright (c) 2021 ReplayMod contributors
 * Copyright (c) 2021 Sk1er LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.replaymod.core.gui.common.elementa

import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.dsl.*
import gg.essential.universal.UMatrixStack
import java.awt.Color

open class UITextInput @JvmOverloads constructor(
    placeholder: String = "",
    shadow: Boolean = true,
    selectionBackgroundColor: Color = Color.WHITE,
    selectionForegroundColor: Color = Color(64, 139, 229),
    allowInactiveSelection: Boolean = false,
    inactiveSelectionBackgroundColor: Color = Color(176, 176, 176),
    inactiveSelectionForegroundColor: Color = Color.WHITE,
    cursorColor: Color = Color.WHITE
) : AbstractTextInput(
    placeholder,
    shadow,
    selectionBackgroundColor,
    selectionForegroundColor,
    allowInactiveSelection,
    inactiveSelectionBackgroundColor,
    inactiveSelectionForegroundColor,
    cursorColor = Color.WHITE
) {
    protected var minWidth: WidthConstraint? = null
    protected var maxWidth: WidthConstraint? = null

    protected val placeholderWidth = placeholder.width()

    fun setMinWidth(constraint: WidthConstraint) = apply {
        minWidth = constraint
    }

    fun setMaxWidth(constraint: WidthConstraint) = apply {
        maxWidth = constraint
    }

    override fun getText() = textualLines.first().text

    protected open fun getTextForRender(): String = getText()

    protected open fun setCursorPos() {
        cursorComponent.unhide()
        val (cursorPosX, _) = cursor.toScreenPos()
        cursorComponent.setX((cursorPosX).pixels())
    }

    override fun textToLines(text: String): List<String> {
        return listOf(text.replace('\n', ' '))
    }

    override fun scrollIntoView(pos: LinePosition) {
        val column = pos.column
        val lineText = getTextForRender()
        if (column < 0 || column > lineText.length)
            return

        val widthBeforePosition = lineText.substring(0, column).width(getTextScale())

        when {
            getTextForRender().width(getTextScale()) < getWidth() -> {
                horizontalScrollingOffset = 0f
            }
            horizontalScrollingOffset > widthBeforePosition -> {
                horizontalScrollingOffset = widthBeforePosition
            }
            widthBeforePosition - horizontalScrollingOffset > getWidth() -> {
                horizontalScrollingOffset = widthBeforePosition - getWidth()
            }
        }
    }

    override fun screenPosToVisualPos(x: Float, y: Float): LinePosition {
        val targetXPos = x + horizontalScrollingOffset
        var currentX = 0f

        val line = getTextForRender()

        for (i in line.indices) {
            val charWidth = line[i].width(getTextScale())
            if (currentX + (charWidth / 2) >= targetXPos) return LinePosition(0, i, isVisual = true)
            currentX += charWidth
        }

        return LinePosition(0, line.length, isVisual = true)
    }

    override fun recalculateDimensions() {
        if (minWidth != null && maxWidth != null) {
            val width = if (!hasText() && !this.active) {
                placeholderWidth
            } else {
                getTextForRender().width(getTextScale()) + 1 /* cursor */
            }
            setWidth(width.pixels().coerceIn(minWidth!!, maxWidth!!))
        }
    }

    override fun splitTextForWrapping(text: String, maxLineWidth: Float): List<String> {
        return listOf(text)
    }

    override fun onEnterPressed() {
        activateAction(getText())
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)

        if (!active && !hasText()) {
            getFontProvider().drawString(matrixStack, placeholder, getColor(), getLeft(), getTop(), 10f, getTextScale())
            return super.draw(matrixStack)
        }

        val lineText = getTextForRender()

        if (hasSelection()) {
            var currentX = getLeft()
            cursorComponent.hide(instantly = true)

            if (!selectionStart().isAtLineStart) {
                val preSelectionText = lineText.substring(0, selectionStart().column)
                drawUnselectedTextCompat(matrixStack, preSelectionText, currentX, row = 0)
                currentX += preSelectionText.width(getTextScale())
            }

            val selectedText = lineText.substring(selectionStart().column, selectionEnd().column)
            val selectedTextWidth = selectedText.width(getTextScale())
            drawSelectedTextCompat(matrixStack, selectedText, currentX, currentX + selectedTextWidth, row = 0)
            currentX += selectedTextWidth

            if (!selectionEnd().isAtLineEnd) {
                drawUnselectedTextCompat(matrixStack, lineText.substring(selectionEnd().column), currentX, row = 0)
            }
        } else {
            cursorComponent.setY(basicYConstraint {
                getTop()
            })
            setCursorPos()

            drawUnselectedTextCompat(matrixStack, lineText, getLeft(), 0)
        }

        super.draw(matrixStack)
    }
}
