package com.replaymod.core.gui.common.timeline

import com.replaymod.core.gui.common.UI9Slice
import com.replaymod.core.gui.common.elementa.UIScrollComponent
import com.replaymod.core.gui.utils.actualHorizontalOffset
import com.replaymod.core.gui.utils.addTooltip
import com.replaymod.core.gui.utils.pollingState
import com.replaymod.core.gui.utils.selfOrParentOfType
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.pixels
import gg.essential.universal.UKeyboard
import net.minecraft.util.Identifier
import kotlin.math.pow
import kotlin.time.Duration

class UITimeline : UIContainer() {
    val length = BasicState(Duration.ZERO)
    val offset = BasicState(Duration.ZERO)
    val zoom = BasicState(1f)
    private val widthOfView = pollingState(1f) { content.getWidth() }
    private val widthOfContent = widthOfView.zip(zoom).map { (viewWidth, zoom) -> viewWidth / zoom }
    val unit = length.zip(widthOfContent).map { (length, width) -> length / width.toDouble() }

    val lengthMillis get() = length.get().inWholeMilliseconds

    val background by UI9Slice(TEXTURE, UI9Slice.TextureData.ofSize(64, 22, 5, 5, 5, 5).offset(0, 16)).constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    val content by UIScrollComponent(horizontalScrollEnabled = true, verticalScrollEnabled = false).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 100.percent - 6.pixels
        height = 100.percent - 6.pixels
    } childOf this

    val contentSize by UIContainer().constrain {
        width = widthOfContent.pixels()
    } childOf content

    val indicators by UITimelineIndicators(this).constrain {
        y = 0.pixels(alignOpposite = true)
        width = widthOfContent.pixels()
        height = 6.pixels
    }

    val cursor by UITimelineCursor(this) childOf content

    init {
        addTooltip {
            addLine {
                bindText(pollingState("") {
                    val time = getTimeAt(getMousePosition().first)
                    "%02d:%02d".format(time.inWholeMinutes, time.inWholeSeconds % 60)
                })
            }
        }
    }

    fun getTimeAt(mouseX: Float): Duration {
        val width = content.getWidth()
        val innerX = (mouseX - content.getLeft()).coerceIn(0f, width) - content.actualHorizontalOffset
        return Duration.milliseconds((unit.get() * innerX.toDouble()).inWholeMilliseconds)
    }

    fun enableIndicators() = apply {
        content.insertChildAt(indicators, 0)
    }

    fun enableZooming() = apply {
        content.mouseScrollListeners.clear()
        onMouseScroll { event ->
            event.stopImmediatePropagation()

            if (UKeyboard.isCtrlKeyDown()) {
                val (absoluteMouseX, _) = getMousePosition()
                val mouseX = absoluteMouseX - content.getLeft()
                val fixedPos = mouseX - content.horizontalOffset
                val oldZoom = zoom.get()
                val newZoom = (oldZoom * (1.2).pow(-event.delta).toFloat()).coerceIn(0.001f, 1f)
                val zoomedPos = fixedPos * oldZoom / newZoom
                val newOffset = mouseX - zoomedPos
                zoom.set(newZoom)
                content.onWindowResize()
                content.scrollTo(horizontalOffset = newOffset, smoothScroll = false)
            } else {
                content.onScroll(event.delta.toFloat(), isHorizontal = true)
            }
        }
    }

    class Constraint(value: Duration) : XConstraint, WidthConstraint {
        override var cachedValue = 0f
        override var recalculate = true
        override var constrainTo: UIComponent? = null

        private var valueState: State<Duration> = BasicState(value)

        var value: Duration
            get() = valueState.get()
            set(value) {
                valueState.set(value)
            }

        fun bindValue(newState: State<Duration>) = apply {
            valueState = newState
        }

        override fun getXPositionImpl(component: UIComponent): Float = getImpl(component, true)
        override fun getWidthImpl(component: UIComponent): Float = getImpl(component, false)

        private fun getImpl(component: UIComponent, absolute: Boolean): Float {
            val targetComponent = constrainTo ?: component.parent
            val targetTimeline = targetComponent.selfOrParentOfType<UITimeline>()
                ?: throw IllegalStateException("$targetComponent needs to be the child of a timeline")
            val targetContent = targetTimeline.content
            val unit = targetTimeline.unit.get()

            val value = this.valueState.get()

            val offset = (value / unit).toFloat()
            return if (absolute) {
                targetContent.getLeft() + targetContent.actualHorizontalOffset + offset
            } else {
                offset
            }
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
            when (type) {
                ConstraintType.X -> visitor.visitParent(ConstraintType.X)
                ConstraintType.WIDTH -> visitor.visitParent(ConstraintType.WIDTH)
                else -> throw IllegalArgumentException(type.prettyName)
            }
        }
    }

    companion object {
        internal val TEXTURE = Identifier("jgui", "gui.png")
    }
}