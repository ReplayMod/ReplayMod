package com.replaymod.core.gui.common

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.GradientComponent.Companion.drawGradientBlock
import gg.essential.elementa.components.GradientComponent.GradientDirection.TOP_TO_BOTTOM
import gg.essential.elementa.components.UIBlock.Companion.drawBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.universal.UMatrixStack
import java.awt.Color

class UITooltip : UIComponent() {

    init {
        constrain {
            width = ChildBasedMaxSizeConstraint() + 8.pixels
            height = ChildBasedSizeConstraint() + 8.pixels
        }
    }

    val content by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf this

    fun addLine(text: String = "", configure: UIText.() -> Unit = {}) = apply {
        val component = UIText(text).constrain {
            y = SiblingConstraint(padding = 3f)
        } childOf content
        component.configure()
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val l = getLeft().toDouble()
        val r = getRight().toDouble()
        val t = getTop().toDouble()
        val b = getBottom().toDouble()

        // Draw background
        drawBlock(matrixStack, BACKGROUND_COLOR, l + 1, t, r - 1, b) // Top to bottom
        drawBlock(matrixStack, BACKGROUND_COLOR, l, t + 1, l + 1, b - 1) // Left pixel row
        drawBlock(matrixStack, BACKGROUND_COLOR, r - 1, t + 1, r, b - 1) // Right pixel row

        // Draw the border, it gets darker from top to bottom
        drawBlock(matrixStack, BORDER_LIGHT, l + 1, t + 1, r - 1, t + 2) // Top border
        drawBlock(matrixStack, BORDER_DARK, l + 1, b - 2, r - 1, b - 1) // Bottom border
        drawGradientBlock(matrixStack, l + 1, t + 2, l + 2, b - 2, BORDER_LIGHT, BORDER_DARK, TOP_TO_BOTTOM) // Left border
        drawGradientBlock(matrixStack, r - 2, t + 2, r - 1, b - 2, BORDER_LIGHT, BORDER_DARK, TOP_TO_BOTTOM) // Right border

        super.draw(matrixStack)
    }

    companion object {
        private val BACKGROUND_COLOR = Color(16, 0, 16, 240)
        private val BORDER_LIGHT = Color(80, 0, 255, 80)
        private val BORDER_DARK = Color(40, 0, 127, 80)
    }
}