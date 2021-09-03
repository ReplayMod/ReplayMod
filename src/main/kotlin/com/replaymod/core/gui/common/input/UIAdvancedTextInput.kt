package com.replaymod.core.gui.common.input

import com.replaymod.core.gui.common.elementa.UITextInput
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels

open class UIAdvancedTextInput(
    private val preferReplace: Boolean = true,
    private val alignRight: Boolean = true,
) : UITextInput() {

    override fun afterInitialization() {
        super.afterInitialization()

        if (alignRight) {
            constrain {
                x = 2.pixels(alignOpposite = true)
            }
        }
    }

    protected open fun shouldReplaceExistingText(add: AddTextOperation) = preferReplace
    protected open fun fixText(): TextOperation? = null

    override fun commitTextOperation(operation: TextOperation) {
        var op = operation

        if (op is AddTextOperation && shouldReplaceExistingText(op)) {
            op = CompoundOperation(op, with(op) {
                applied { RemoveTextOperation(endPos, endPos.offsetColumn(newText.length), false) }
            })
        }

        op.applied {
            val fix = fixText() ?: return
            if (fix !is NoOperation) {
                op = CompoundOperation(op, fix)
            }
        }

        super.commitTextOperation(op)
    }

    private inner class CompoundOperation(
        val inner: TextOperation,
        val fix: TextOperation,
    ) : TextOperation() {
        override fun redo() {
            inner.redo()

            val orgCursor = cursor
            val orgOtherSelectionEnd = otherSelectionEnd
            fix.redo()
            cursor = orgCursor.coerceInBounds()
            otherSelectionEnd = orgOtherSelectionEnd.coerceInBounds()
        }

        override fun undo() {
            fix.undo()
            inner.undo()
        }
    }

    protected inner class NoOperation : TextOperation() {
        override fun redo() = Unit
        override fun undo() = Unit
    }

    private inline fun <T> TextOperation?.applied(block: () -> T): T {
        this?.redo()
        try {
            return block()
        } finally {
            this?.undo()
        }
    }

    private fun LinePosition.coerceInBounds(): LinePosition {
        val lines = if (isVisual) visualLines else textualLines
        val line = line.coerceIn(0, lines.lastIndex)
        return LinePosition(line, column.coerceIn(0, lines[line].length), isVisual)
    }

    protected val AddTextOperation.endPos get() = startPos.offsetColumn(newText.length)
}