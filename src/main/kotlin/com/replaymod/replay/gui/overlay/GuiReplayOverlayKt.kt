package com.replaymod.replay.gui.overlay

import com.replaymod.core.gui.utils.hiddenChildOf
import com.replaymod.replay.gui.overlay.panels.UIHotkeyButtonsPanel
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*

class GuiReplayOverlayKt {
    val window = Window(ElementaVersion.V1, 60)

    val bottomRightPanel by UIContainer().constrain {
        x = 6.pixels(alignOpposite = true)
        y = 6.pixels(alignOpposite = true)
        // Children will be on top of each other
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf window

    val hotkeyButtonsPanel by UIHotkeyButtonsPanel().apply {
        toggleButton childOf bottomRightPanel
    } hiddenChildOf window
}
