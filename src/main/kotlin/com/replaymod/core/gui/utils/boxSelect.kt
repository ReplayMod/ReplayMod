package com.replaymod.core.gui.utils

import com.replaymod.core.gui.utils.Box.Companion.toBox
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.Window
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface BoxSelectable

/**
 * Enables box selections to be draw anywhere within this component (usually the window).
 * This is safe to call multiple times on the same component (e.g. for the window by independent code).
 */
fun <T : UIComponent> T.onBoxSelection(onChange: (selected: List<BoxSelectable>, final: Boolean) -> Unit) = apply {
    var currentBoxSelect: BoxSelect? = null

    onLeftClick { event ->
        event.stopPropagation()
        currentBoxSelect = BoxSelect(event.absoluteX to event.absoluteY)
    }
    onMouseDrag { mouseX, mouseY, _ ->
        val boxSelect = currentBoxSelect ?: return@onMouseDrag

        boxSelect.second = (mouseX + getLeft()) to (mouseY + getTop())
        val box = boxSelect.box

        if (!boxSelect.passedThreshold) {
            if (box.width < 3 && box.height < 3) {
                return@onMouseDrag
            } else {
                boxSelect.passedThreshold = true

                if (effects.find { it is BoxSelectionEffect } == null) {
                    enableEffect(BoxSelectionEffect(boxSelect))
                }
            }
        }
        onChange(findAllInBox(box), false)
    }
    onMouseRelease {
        val boxSelect = currentBoxSelect?.also { currentBoxSelect = null } ?: return@onMouseRelease
        if (!boxSelect.passedThreshold) {
            return@onMouseRelease
        }
        removeEffect<BoxSelectionEffect>()
        onChange(findAllInBox(boxSelect.box), true)
    }
}

private data class BoxSelect(
    var first: Pair<Float, Float>,
    var second: Pair<Float, Float> = first,
    var passedThreshold: Boolean = false,
) {
    val box: Box
        get() {
            val (x1, y1) = first
            val (x2, y2) = second
            return Box.fromBounds(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
        }
}

/** Box around some center point (x, y). Note that (x, y) is **not** the top left point here, it is the center. */
private data class Box(val x: Float, val y: Float, val width: Float, val height: Float) {
    val left get() = x - width / 2
    val top get() = y - height / 2
    val right get() = x + width / 2
    val bottom get() = y + height / 2

    private fun intersectsX(other: Box) = abs(this.x - other.x) * 2 < this.width + other.width
    private fun intersectsY(other: Box) = abs(this.y - other.y) * 2 < this.height + other.height
    fun intersects(other: Box) = intersectsX(other) && intersectsY(other)

    companion object {
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): Box {
            val width = right - left
            val height = bottom - top
            return Box(left + width / 2, top + height / 2, width, height)
        }

        fun UIComponent.toBox() = fromBounds(getLeft(), getTop(), getRight(), getBottom())
    }
}

private val dummyMatrixStack = UMatrixStack()

private fun UIComponent.findAllInBox(box: Box): List<BoxSelectable> {
    val parentWindow = Window.of(this)
    val result = mutableListOf<BoxSelectable>()

    fun UIComponent.collect() {
        if (!box.intersects(this.toBox())) {
            return
        }

        if (this is BoxSelectable) {
            result.add(this)
        }

        // Window.isAreaVisible uses the scissor effect to determine whether an area is visible
        val scissors = effects.filterIsInstance<ScissorEffect>()
        scissors.forEach { it.beforeDraw(dummyMatrixStack) }

        for (child in children) {
            if (alwaysDrawChildren() || parentWindow.isAreaVisible(
                    child.getLeft().toDouble(),
                    child.getTop().toDouble(),
                    child.getRight().toDouble(),
                    child.getBottom().toDouble()
                )
            ) {
                child.collect()
            }
        }

        scissors.forEach { it.afterDraw(dummyMatrixStack) }
    }
    collect()

    return result
}

/**
 * Draws a highlight and border for the given box selection on top of a component.
 */
private class BoxSelectionEffect(val boxSelect: BoxSelect) : Effect() {
    override fun afterDraw(matrixStack: UMatrixStack) {
        val box = boxSelect.box
        val l = box.left.toDouble()
        val t = box.top.toDouble()
        val r = box.right.toDouble()
        val b = box.bottom.toDouble()
        val w = box.width.toDouble()
        val h = box.height.toDouble()
        val d = 1.0 / UResolution.scaleFactor // a single (real) pixel

        // Background
        UIBlock.drawBlock(matrixStack, Color.YELLOW.withAlpha(0.3f), l, t, r, b)

        // Outline
        UIBlock.drawBlockSized(matrixStack, Color.YELLOW, l, t, d, h) // left
        UIBlock.drawBlockSized(matrixStack, Color.YELLOW, l, t, w, d) // top
        UIBlock.drawBlockSized(matrixStack, Color.YELLOW, r, t, d, h) // right
        UIBlock.drawBlockSized(matrixStack, Color.YELLOW, l, b, w, d) // bottom
    }
}
