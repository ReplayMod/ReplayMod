package com.replaymod.core.gui.utils

import com.replaymod.core.gui.common.UITooltip
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.MousePositionConstraint
import gg.essential.elementa.dsl.*

fun <T : UIComponent> T.addTooltip(configure: UITooltip.() -> Unit) = addTooltip(UITooltip().apply(configure))

fun <T : UIComponent> T.addTooltip(tooltip: UIComponent) = apply {
    tooltip.constrain {
        // Slightly right of the cursor but never off-screen
        x = (MousePositionConstraint() + 8.pixels)
            .coerceAtMost(100.percentOfWindow - basicXConstraint { tooltip.getWidth() })
        // Slightly below the cursor except when there is insufficient space, then slightly above it
        y = basicYConstraint {
            val mouseY = MousePositionConstraint().getYPosition(it)
            val idealY = mouseY + 8
            val height = tooltip.getHeight()
            if (idealY + height <= 100.percentOfWindow.getYPosition(it)) {
                idealY
            } else {
                mouseY - 8 - height
            }
        }
    }

    var unregister: (() -> Unit)? = null

    onMouseEnter {
        Window.enqueueRenderOperation {
            tooltip childOf Window.of(this)
            tooltip.setFloating(true)
            val unregisterOnRemoved = this.onRemoved {
                unregister?.invoke()
                unregister = null
            }
            unregister = {
                unregisterOnRemoved.invoke()
                Window.enqueueRenderOperation {
                    tooltip.setFloating(false)
                    tooltip.hide(true)
                }
            }
        }
    }
    onMouseLeave {
        unregister?.invoke()
        unregister = null
    }
}
