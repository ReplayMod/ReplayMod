package com.replaymod.core.gui.common.input

import com.replaymod.core.gui.common.elementa.UITextInput
import com.replaymod.core.gui.utils.enableTabFocusChange
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import java.awt.Color

class UIInputField<T : UITextInput>(
    val input: T,
) : UIContainer() {

    private val focused = BasicState(false)

    val background by UIBlock(Color.BLACK).constrain {
        width = 100.percent
        height = 100.percent
    }.effect(OutlineEffect(Color.WHITE, 1f, drawInsideChildren = true).bindColor(focused.map { focused ->
        if (focused) {
            Color.WHITE
        } else {
            Color(160, 160, 160)
        }
    })).onMouseClick { event ->
        for (listener in input.mouseClickListeners) {
            input.listener(event.projectOnto(input))
        }
    } childOf this

    init {
        constrain {
            width = 200.pixels
            height = 20.pixels
        }

        input.constrain {
            x = 3.pixels
            y = CenterConstraint()
        }.apply {
            setMinWidth(1.pixel)
            setMaxWidth(100.percent - 6.pixels)
            enableTabFocusChange()
        }.onFocus {
            focused.set(true)
        }.onFocusLost {
            focused.set(false)
        }.onMouseClick { event ->
            input.grabWindowFocus()

            if (!input.isActive()) {
                Window.enqueueRenderOperation {
                    if (input.isActive()) {
                        for (listener in input.mouseClickListeners) {
                            listener(event)
                        }
                    }
                }
            }
        } childOf this
    }

    companion object {
        fun text(
            placeholder: String = "",
        ): UIInputField<UITextInput> = UIInputField(UITextInput(
            placeholder,
        ))
    }

    private fun UIClickEvent.projectOnto(component: UIComponent) = copy(
        absoluteX = absoluteX.coerceIn(component.getLeft(), component.getRight()),
        absoluteY = absoluteY.coerceIn(component.getTop(), component.getBottom()),
        currentTarget = component,
    )
}