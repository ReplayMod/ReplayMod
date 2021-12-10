package com.replaymod.core.gui.common.scrollbar

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UMatrixStack
import java.awt.Color

class UIFlatScrollBar(transparent: Boolean) : UIBlock(Color.BLACK.withAlpha(if(transparent) 0.5f else 1f)) {
    val grip by Grip().constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    init {
        constrain {
            width = 100.percent
            height = 100.percent
        }
    }

    class Grip : UIComponent() {
        override fun draw(matrixStack: UMatrixStack) {
            beforeDraw(matrixStack)

            val l = getLeft().toDouble()
            val r = getRight().toDouble()
            val t = getTop().toDouble()
            val b = getBottom().toDouble()

            // Slider
            drawBlock(matrixStack, Color.LIGHT_GRAY, l, t, r, b)

            // Right shadow
            drawBlock(matrixStack, Color.GRAY, r - 1, t, r, b)

            // Bottom shadow
            drawBlock(matrixStack, Color.GRAY, l, b - 1, r, b)

            super.draw(matrixStack)
        }
    }
}