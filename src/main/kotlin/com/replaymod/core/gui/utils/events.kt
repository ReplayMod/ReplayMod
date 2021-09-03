package com.replaymod.core.gui.utils

import gg.essential.elementa.UIComponent
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.utils.ObservableClearEvent
import gg.essential.elementa.utils.ObservableRemoveEvent
import java.util.Observer

fun <T : UIComponent> T.onAnimationFrame(block: T.() -> Unit) = apply {
    enableEffect(object : Effect() {
        override fun animationFrame() = block()
    })
}

fun <T : UIComponent> T.onLeftClick(clickCount: Int? = null, handler: T.(event: UIClickEvent) -> Unit) = apply {
    onMouseClick { if (it.mouseButton == 0 && (clickCount == null || it.clickCount == clickCount)) handler(it) }
}

fun <T : UIComponent> T.onRightClick(clickCount: Int? = null, handler: T.(event: UIClickEvent) -> Unit) = apply {
    onMouseClick { if (it.mouseButton == 1 && (clickCount == null || it.clickCount == clickCount)) handler(it) }
}

fun <T : UIComponent> T.onMiddleClick(clickCount: Int? = null, handler: T.(event: UIClickEvent) -> Unit) = apply {
    onMouseClick { if (it.mouseButton == 2 && (clickCount == null || it.clickCount == clickCount)) handler(it) }
}

private fun <T : UIComponent> T.onMouse(mouseButton: Int, handler: T.(mouseX: Float, mouseY: Float) -> Unit) = apply {
    var dragging = false
    onMouseClick {
        if (it.mouseButton == mouseButton) {
            dragging = true
            handler(it.absoluteX, it.absoluteY)
        }
    }
    onMouseDrag { mouseX, mouseY, _ -> if (dragging) handler(getLeft() + mouseX, getTop() + mouseY) }
    onMouseRelease { dragging = false }
}

fun <T : UIComponent> T.onLeftMouse(handler: T.(mouseX: Float, mouseY: Float) -> Unit) = onMouse(0, handler)
fun <T : UIComponent> T.onRightMouse(handler: T.(mouseX: Float, mouseY: Float) -> Unit) = onMouse(1, handler)
fun <T : UIComponent> T.onMiddleMouse(handler: T.(mouseX: Float, mouseY: Float) -> Unit) = onMouse(2, handler)

// Elementa has no unmount event, so instead we listen for changes to the children list of all our parents.
fun UIComponent.onRemoved(listener: () -> Unit): () -> Unit {
    if (parent == this) {
        return {}
    }
    val parentUnregister = parent.onRemoved(listener)

    val observer = Observer { _, event ->
        if (event is ObservableClearEvent<*> || event is ObservableRemoveEvent<*> && event.element.value == this) {
            listener()
        }
    }
    parent.children.addObserver(observer)
    return {
        parent.children.deleteObserver(observer)
        parentUnregister()
    }
}
