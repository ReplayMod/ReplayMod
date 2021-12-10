package com.replaymod.core.gui.utils

import com.replaymod.core.gui.common.elementa.UIScrollComponent
import gg.essential.elementa.UIComponent
import gg.essential.elementa.dsl.*

infix fun <T : UIComponent> T.hiddenChildOf(parent: UIComponent) = (this childOf parent).also { this.hide(true) }

inline fun <reified T : UIComponent> UIComponent.parentOfType(): T? =
    if (hasParent && parent != this) parent.selfOrParentOfType() else null

inline fun <reified T : UIComponent> UIComponent.selfOrParentOfType(): T? {
    var component: UIComponent = this
    while (component.hasParent && component.parent != component) {
        if (component is T) {
            return component
        }
        component = component.parent
    }
    return null
}

/** Like [UIScrollComponent.horizontalOffset] but including any animations. */
val UIScrollComponent.actualHorizontalOffset
    get() = children.first().getLeft() - getLeft()
