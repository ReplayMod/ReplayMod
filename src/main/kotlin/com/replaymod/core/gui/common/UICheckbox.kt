package com.replaymod.core.gui.common

import com.replaymod.core.gui.utils.Resources
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.invisible
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UMatrixStack
import java.awt.Color

class UICheckbox : UIComponent() {

    private val hovered = BasicState(false)
    private val enabled = BasicState(true)
    val checked = BasicState(false)

    var isChecked: Boolean
        get() = checked.get()
        set(value) = checked.set(value)

    private val checkmark by UITexture(Resources.icon("checkmark"), UITexture.TextureData.full()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 9.pixels
        height = 9.pixels
        color = checked.zip(enabled).map { (checked, enabled) ->
            if (enabled) {
                Color.WHITE.withAlpha(if (checked) 1f else 0f)
            } else {
                if (checked) Color.GRAY else Color.DARK_GRAY
            }
        }.toConstraint()
    } childOf this

    init {
        onMouseEnter { hovered.set(true) }
        onMouseLeave { hovered.set(false) }

        onMouseClick {
            if (!enabled.get()) return@onMouseClick
            UIButton.playClickSound()
            checked.set { !it }
        }

        constrain {
            width = 9.pixels
            height = 9.pixels
            color = Color.WHITE.invisible().toConstraint()
        }
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val left = getLeft().toDouble()
        val right = getRight().toDouble()
        val top = getTop().toDouble()
        val bottom = getBottom().toDouble()

        // Outline
        val outlineColor = if (hovered.get() && enabled.get()) Color.WHITE else Color.BLACK
        UIBlock.drawBlock(matrixStack, outlineColor, left, top, right, bottom)

        // Background
        val backgroundColor = Color.DARK_GRAY.darker()
        UIBlock.drawBlock(matrixStack, backgroundColor, left + 1, top + 1, right - 1, bottom - 1)

        // Checkmark
        super.draw(matrixStack)
    }
}