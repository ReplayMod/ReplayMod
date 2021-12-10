package com.replaymod.core.gui.common.timeline

import com.replaymod.core.gui.utils.actualHorizontalOffset
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.universal.UMatrixStack
import java.awt.Color
import kotlin.time.Duration

class UITimelineIndicators(val timeline: UITimeline) : UIComponent() {
    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val height = getHeight()
        val bottom = getBottom()

        val unit = timeline.unit.get()
        val visibleLength = unit * timeline.content.getWidth().toDouble()
        val offset = unit * -timeline.content.actualHorizontalOffset.toDouble()
        val interval = getInterval()
        val smallInterval = interval / 5
        var time = interval * (offset / interval).toInt()
        var counter = 0
        while (time <= offset + visibleLength) {
            if (time >= offset) {
                val big = counter % 5 == 0
                val color = if (big) Color.LIGHT_GRAY else Color.WHITE
                val x = UITimeline.Constraint(time).getXPosition(this) - 0.5
                val h = height / if (big) 1.0 else 2.0
                UIBlock.drawBlockSized(matrixStack, color, x, bottom - h, 1.0, h)
            }
            counter++
            time += smallInterval
        }

        super.draw(matrixStack)
    }

    internal fun getInterval(): Duration {
        val width = timeline.content.getWidth().toDouble() // Width of the drawn timeline
        val segmentLength = timeline.unit.get() * width // Length of the drawn timeline
        val minDistance = MIN_DISTANCE * if (timeline.length.get() > Duration.Companion.hours(1)) 1.2 else 1.0
        val maxIndicators = width / minDistance // Max. amount of indicators that can fit in the timeline
        val minInterval = segmentLength / maxIndicators // Min. interval between those indicators
        return SNAP_TO.firstOrNull { it > minInterval } ?: SNAP_TO.last() // find next greater snap, fallback to max one
    }

    companion object {
        private const val MIN_DISTANCE = 40

        private val SNAP_TO = listOf(
            Duration.seconds(1),
            Duration.seconds(2),
            Duration.seconds(5),
            Duration.seconds(10),
            Duration.seconds(15),
            Duration.seconds(20),
            Duration.seconds(30),
            Duration.minutes(1),
            Duration.minutes(2),
            Duration.minutes(5),
            Duration.minutes(10),
            Duration.minutes(15),
            Duration.minutes(30),
            Duration.hours(1),
            Duration.hours(2),
            Duration.hours(5),
            Duration.hours(10),
        )
    }
}