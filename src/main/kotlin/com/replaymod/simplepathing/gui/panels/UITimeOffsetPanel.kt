package com.replaymod.simplepathing.gui.panels

import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.common.UICheckbox
import com.replaymod.core.gui.common.input.*
import com.replaymod.core.gui.utils.*
import com.replaymod.core.utils.i18n
import com.replaymod.simplepathing.gui.KeyframeState
import com.replaymod.simplepathing.gui.moveKeyframes
import com.replaymod.simplepathing.gui.moveTimeKeyframes
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.utils.withAlpha
import java.awt.Color
import kotlin.time.Duration

class UITimeOffsetPanel(
    val state: KeyframeState,
    timePanel: UITimePanel,
) : UIBlock(Color(32, 32, 32).withAlpha(0.6f)) {

    init {
        timePanel.open.onSetValue { if (it) unhide() else hide(true) }

        constrain {
            height = ChildBasedSizeConstraint() + 10.pixels
        }
    }

    val container by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 100.percent - 10.pixels
        height = ChildBasedSizeConstraint()
    } childOf this

    val label by UIContainer().constrain {
        width = 100.percent
        height = 14.pixels
    }.addChild(UIText("Shift time (min:sec:ms)".i18n()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }) childOf container

    val row by UIContainer().constrain {
        x = CenterConstraint()
        y = SiblingConstraint(2f)
        width = ChildBasedSizeConstraint()
        height = 20.pixels
    } childOf container

    val minusButton by UIButton().texture(Resources.icon("minus")).constrain {
        width = 20.pixels
        height = 20.pixels
    }.onLeftClick {
        shiftTime(-1)
    } childOf row

    val plusButton by UIButton().texture(Resources.icon("plus")).constrain {
        x = SiblingConstraint(3f)
        width = 20.pixels
        height = 20.pixels
    }.onLeftClick {
        shiftTime(1)
    } childOf row

    val timeField by UIInputOrExpressionField.forTimeInput(UITimeInput(minuteDigits = 2)).constrain {
        x = SiblingConstraint(5f)
        width = 54.pixels
        height = 20.pixels
    }.apply {
        input.value = Duration.milliseconds(100)
    } childOf row

    val checkboxes by UIContainer().constrain {
        x = SiblingConstraint(5f)
        width = ChildBasedMaxSizeConstraint()
        height = 100.percent
    } childOf row

    val replayTimeCheckbox by UICheckbox().constrain {
    }.apply {
        isChecked = true
    }.addTooltip {
        addLine("Replay Time")
    } childOf checkboxes

    val videoTimeCheckbox by UICheckbox().constrain {
        y = 0.pixels(alignOpposite = true)
    }.apply {
        isChecked = true
    }.addTooltip {
        addLine("Video Time")
    } childOf checkboxes

    private fun shiftTime(direction: Int) {
        val deltaTime = timeField.input.value * direction

        if (replayTimeCheckbox.isChecked) {
            if (state.selection.get().size > 0) {
                val change = state.moveTimeKeyframes(state.selection.get(), deltaTime)
                state.mod.currentTimeline.timeline.pushChange(change)
            } else {
                with(state.gui.replayHandler) {
                    val newTime = replaySender.currentTimeStamp() + deltaTime.inWholeMilliseconds
                    doJump(newTime.toInt(), true)
                }
            }
        }

        if (videoTimeCheckbox.isChecked) {
            if (state.selection.get().size > 0) {
                val change = state.moveKeyframes(state.selection.get(), deltaTime)
                state.mod.currentTimeline.timeline.pushChange(change)
            } else {
                state.gui.timeline.cursor.position.set { it + deltaTime }
            }
        }
    }
}