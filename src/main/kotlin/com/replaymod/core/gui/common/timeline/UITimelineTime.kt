package com.replaymod.core.gui.common.timeline

import com.replaymod.core.gui.utils.actualHorizontalOffset
import gg.essential.elementa.UIComponent
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.width
import gg.essential.universal.UMatrixStack
import kotlin.time.Duration

class UITimelineTime(val timeline: UITimeline) : UIComponent() {
    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val left = getLeft()
        val right = getRight()
        val top = getTop()
        val fontProvider = getFontProvider()
        val color = getColor()

        val unit = timeline.unit.get()
        val visibleLength = unit * timeline.content.getWidth().toDouble()
        val offset = unit * -timeline.content.actualHorizontalOffset.toDouble()
        val interval = timeline.indicators.getInterval()
        var time = interval * (offset / interval).toInt()
        while (time <= offset + visibleLength) {
            if (time >= offset) {
                val str = if (timeline.length.get() > Duration.hours(1)) {
                    "%02d:%02d:%02d".format(time.inWholeHours, time.inWholeMinutes % 60, time.inWholeSeconds % 60)
                } else {
                    "%02d:%02d".format(time.inWholeMinutes, time.inWholeSeconds % 60)
                }
                val halfStrWidth = str.width(fontProvider = fontProvider) / 2
                val x = (UITimeline.Constraint(time).boundTo(timeline).getXPosition(this) - 0.5f)
                    .coerceIn(left + halfStrWidth, right - halfStrWidth)
                fontProvider.drawString(matrixStack, str, color, x - halfStrWidth, top, 1f, 1f)
            }
            time += interval
        }

        super.draw(matrixStack)
    }
}