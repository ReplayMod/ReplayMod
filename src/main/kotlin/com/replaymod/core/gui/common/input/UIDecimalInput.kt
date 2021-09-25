package com.replaymod.core.gui.common.input

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat

class UIDecimalInput(
    precision: Int = 5,
) : UIAdvancedTextInput() {
    private val mathContext = MathContext(precision)
    private val format = DecimalFormat("0." + "0".repeat(precision)).apply {
        minimumFractionDigits = precision
        maximumFractionDigits = precision
    }

    private fun BigDecimal.formatString() = format.format(this)

    private fun String.parseDecimal(): BigDecimal? = try {
        BigDecimal(this, mathContext)
    } catch (e: NumberFormatException) {
        null
    }

    var value: BigDecimal
        get() = getText().parseDecimal() ?: BigDecimal.ZERO
        set(value) {
            val str = value.formatString()
            if (getText() != str) {
                setText(str)
            }
        }

    override fun shouldReplaceExistingText(add: AddTextOperation): Boolean =
        super.shouldReplaceExistingText(add) &&
                (add.newText.length != 1 || getText()[add.startPos.column] != '.' || add.newText == ".")

    override fun fixText(): TextOperation? {
        val unformatted = getText()
        val duration = getText().parseDecimal() ?: return null
        val formatted = duration.formatString()

        if (formatted != unformatted) {
            val start = LinePosition(0, 0, false)
            return ReplaceTextOperation(
                AddTextOperation(formatted, start),
                RemoveTextOperation(start, LinePosition(0, unformatted.length, false), false)
            )
        }

        return NoOperation()
    }

    override fun commitTextOperation(operation: TextOperation) {
        var op = operation

        // If they try to delete the separator, move the cursor before it instead
        if (op is RemoveTextOperation && op.text == ".") {
            cursor = op.startPos
            otherSelectionEnd = cursor
            return
        }

        // If they add the final digit before a separator, move the cursor past the separator as well
        if (op is AddTextOperation && getText().drop(op.endPos.column).startsWith(".")) {
            op = AddTextOperation(op.newText + ".", op.startPos)
        }

        super.commitTextOperation(op)
    }
}