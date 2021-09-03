package com.replaymod.core.gui.common

import de.johni0702.minecraft.gui.GuiRenderer
import de.johni0702.minecraft.gui.RenderInfo
import de.johni0702.minecraft.gui.container.GuiContainer
import de.johni0702.minecraft.gui.element.AbstractGuiElement
import de.johni0702.minecraft.gui.function.Clickable
import de.johni0702.minecraft.gui.function.Draggable
import de.johni0702.minecraft.gui.function.Scrollable
import de.johni0702.minecraft.gui.function.Typeable
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint
import gg.essential.elementa.components.Window
import gg.essential.universal.UMatrixStack

class GuiWindow(parent: GuiContainer<*>, val window: Window) : AbstractGuiElement<GuiWindow>(parent), Clickable, Draggable, Scrollable, Typeable {
    private var dragging = false

    override fun getThis(): GuiWindow = this
    override fun calcMinSize(): ReadableDimension = Dimension()

    override fun draw(renderer: GuiRenderer, size: ReadableDimension, renderInfo: RenderInfo) {
        window.draw(UMatrixStack())
    }

    private fun hasComponent(pos: ReadablePoint): Boolean =
        window.hoveredFloatingComponent != null || window.hitTest(pos.x.toFloat(), pos.y.toFloat()) != window

    override fun mouseClick(position: ReadablePoint, button: Int): Boolean {
        return if (hasComponent(position)) {
            window.mouseClick(position.x.toDouble(), position.y.toDouble(), button)
            true
        } else {
            false
        }.also { dragging = it }
    }

    override fun mouseDrag(position: ReadablePoint, button: Int, timeSinceLastCall: Long): Boolean {
        return dragging
    }

    override fun mouseRelease(position: ReadablePoint, button: Int): Boolean {
        return if (dragging) {
            dragging = false
            window.mouseRelease()
            true
        } else {
            false
        }
    }

    override fun scroll(position: ReadablePoint, dWheel: Int): Boolean {
        return if (hasComponent(position)) {
            window.mouseScroll(dWheel / 120.0)
            true
        } else {
            false
        }
    }

    override fun typeKey(
        mousePosition: ReadablePoint?,
        keyCode: Int,
        keyChar: Char,
        ctrlDown: Boolean,
        shiftDown: Boolean
    ): Boolean {
        return if (window.focusedComponent != null) {
            window.keyType(keyChar, keyCode)
            true
        } else {
            false
        }
    }
}