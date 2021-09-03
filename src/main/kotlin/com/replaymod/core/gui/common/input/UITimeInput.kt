package com.replaymod.core.gui.common.input

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UITimeInput(
    private val minuteDigits: Int? = 3,
) : UIAdvancedTextInput() {

    private fun Duration.formatString() = "%0${minuteDigits}d:%02d:%03d".format(
        Locale.ROOT,
        inWholeMinutes,
        inWholeSeconds % 60,
        inWholeMilliseconds % 1000
    )

    private fun String.parseDuration(): Duration? = this
        .split(":")
        .also { if (it.size != 3) return null }
        .zip(listOf(minuteDigits ?: 1, 2, 3))
        .map { (str, len) -> str.padEnd(len, '0').toIntOrNull() ?: return null }
        .zip(listOf(DurationUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS))
        .map { (int, unit) -> int.toDuration(unit) }
        .reduce(Duration::plus)

    var value: Duration
        get() = getText().parseDuration() ?: Duration.ZERO
        set(value) {
            val str = value.formatString()
            if (getText() != str) {
                setText(str)
            }
        }

    override fun fixText(): TextOperation? {
        val unformatted = getText()
        val duration = getText().parseDuration() ?: return null
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
        if (op is RemoveTextOperation && op.text == ":") {
            cursor = op.startPos
            otherSelectionEnd = cursor
            return
        }

        // If they add the final digit before a separator, move the cursor past the separator as well
        if (op is AddTextOperation && getText().drop(op.endPos.column).startsWith(":")) {
            op = AddTextOperation(op.newText + ":", op.startPos)
        }

        // Replace any non-digits with the separator character, this allows us to skip the separator with any key
        if (op is AddTextOperation && !getText().all { it.isDigit() || it == ':' }) {
            op = AddTextOperation(
                op.newText.map { if (it.isDigit()) it else ':' }.joinToString(),
                op.startPos
            )
        }

        super.commitTextOperation(op)
    }
}