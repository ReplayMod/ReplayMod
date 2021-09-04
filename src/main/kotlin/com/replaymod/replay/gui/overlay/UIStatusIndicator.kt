package com.replaymod.replay.gui.overlay

import com.replaymod.core.ReplayMod
import com.replaymod.core.gui.common.UITexture
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate

class UIStatusIndicator(u: Int, v: Int) : UIContainer() {
    val icon by UITexture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(16, 16).offset(u, v)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 16.pixels
        height = 16.pixels
    } childOf this

    init {
        constrain {
            x = SiblingConstraint(4f)
            y = 0.pixels(alignOpposite = true)
            width = 20.pixels
            height = 20.pixels
        }
    }
}