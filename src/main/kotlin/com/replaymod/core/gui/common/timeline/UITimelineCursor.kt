package com.replaymod.core.gui.common.timeline

import com.replaymod.core.gui.common.UITexture
import com.replaymod.core.gui.common.bounded
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import kotlin.time.Duration

class UITimelineCursor(val timeline: UITimeline) : UIContainer() {
    val position = BasicState(Duration.ZERO).bounded { it.coerceIn(Duration.ZERO..timeline.length.get()) }

    val positionMillis get() = position.get().inWholeMilliseconds

    private val pin by UITexture(UITimeline.TEXTURE, UITexture.TextureData.ofSize(5, 4).offset(64, 0)).constrain {
        width = 5.pixels
        height = 4.pixels
    } childOf this

    private val needle by UITexture(UITimeline.TEXTURE, UITexture.TextureData.ofSize(5, 11).offset(64, 4)).constrain {
        y = 4.pixels
        width = 5.pixels
        height = 100.percent - 4.pixels
    } childOf this

    init {
        constrain {
            x = UITimeline.Constraint(Duration.ZERO).bindValue(position) - (2.5).pixels
            width = 5.pixels
            height = 100.percent
        }
    }

    override fun isPointInside(x: Float, y: Float): Boolean = false // the cursor should never obstruct clicks, etc.

    fun ensureVisibleWithPadding() = ensureVisible(padding = timeline.content.getWidth() / 10)

    fun ensureVisible(padding: Float = 0f) = apply {
        val scroller = timeline.content
        val position = getLeft() - parent.getLeft()
        if (position - padding + scroller.horizontalOffset < 0) {
            scroller.scrollTo(horizontalOffset = -(position - padding), smoothScroll = false)
        } else if (position + padding + scroller.horizontalOffset > scroller.getWidth()) {
            scroller.scrollTo(horizontalOffset = -(position + padding - scroller.getWidth()), smoothScroll = false)
        }
    }
}