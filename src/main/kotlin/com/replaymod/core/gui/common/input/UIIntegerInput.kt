package com.replaymod.core.gui.common.input

import java.util.*

class UIIntegerInput(
    preferReplace: Boolean = true,
    alignRight: Boolean = true,
    private val fixedDigits: Int? = null,
) : UIAdvancedTextInput(
    preferReplace,
    alignRight,
) {
    var value: Int
        get() = getText().toIntOrNull() ?: 0
        set(value) {
            val str = "%0${fixedDigits ?: 1}d".format(Locale.ROOT, value)
            if (getText() != str) {
                setText(str)
            }
        }

    override fun shouldReplaceExistingText(add: AddTextOperation): Boolean =
        super.shouldReplaceExistingText(add) && add.newText != "-"

    override fun fixText(): TextOperation? {
        val text = getText()

        val validValue = text.toIntOrNull() != null
                || text.isEmpty() // allow field to be completely emptied
                || (fixedDigits == null && text == "-") // allow typing of negative numbers into empty field
        if (!validValue) {
            return null
        }

        val targetLen = fixedDigits
        if (targetLen != null) {
            val newLen = text.length
            val endOfLine = LinePosition(0, newLen, false)
            when {
                newLen < targetLen -> return AddTextOperation("0".repeat(targetLen - newLen), endOfLine)
                newLen > targetLen -> return RemoveTextOperation(
                    endOfLine.offsetColumn(targetLen - newLen),
                    endOfLine,
                    false
                )
            }
        }

        return NoOperation()
    }
}