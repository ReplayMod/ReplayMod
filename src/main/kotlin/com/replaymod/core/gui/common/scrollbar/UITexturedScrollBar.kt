package com.replaymod.core.gui.common.scrollbar

import com.replaymod.core.gui.common.UI9Slice
import com.replaymod.core.gui.common.timeline.UITimeline.Companion.TEXTURE
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*

class UITexturedScrollBar : UIContainer() {
    val background by UI9Slice(TEXTURE, UI9Slice.TextureData.ofSize(64, 9, 2).offset(0, 7)).constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    val inner by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 100.percent - 2.pixels
        height = 100.percent - 2.pixels
    } childOf background

    val grip by UI9Slice(TEXTURE, UI9Slice.TextureData.ofSize(62, 7, 2)).constrain {
        width = 100.percent
        height = 100.percent
    } childOf inner

    init {
        constrain {
            width = 100.percent
            height = 100.percent
        }
    }
}