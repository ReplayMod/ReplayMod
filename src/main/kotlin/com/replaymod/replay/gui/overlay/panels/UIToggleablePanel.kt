package com.replaymod.replay.gui.overlay.panels

import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.utils.bindStateTransition
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.transitions.ExpandFromTransition
import gg.essential.elementa.transitions.ShrinkToTransition
import gg.essential.elementa.utils.withAlpha
import java.awt.Color

open class UIToggleablePanel : UIBlock(Color.BLACK.withAlpha(0.6f)) {
    val open = BasicState(false)

    val toggleButton by UIButton().constrain {
        x = 0.pixels(alignOpposite = true)
        y = SiblingConstraint(4f)
        width = 20.pixels
        height = 20.pixels
    }.onMouseClick {
        open.set { !it }
    } as UIButton

    init {
        constrain {
            x = (-2).pixels(alignOpposite = true) boundTo toggleButton
            y = (-2).pixels(alignOpposite = true) boundTo toggleButton
        }

        bindStateTransition(open) { done, _, open ->
            if (open) {
                unhide()
                ExpandFromTransition.Bottom(0.5f, Animations.IN_OUT_EXP).transition(this) {
                    done()
                }

                // Make sure the toggle button is on top of the panel
                toggleButton.setFloating(true)
            } else {
                ShrinkToTransition.Bottom(0.5f, Animations.IN_OUT_EXP, true).transition(this) {
                    hide(true)

                    toggleButton.setFloating(false)

                    done()
                }
            }
        }
    }
}