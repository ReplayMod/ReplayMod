package com.replaymod.simplepathing.gui.panels

import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.common.input.*
import com.replaymod.core.gui.utils.*
import com.replaymod.core.utils.i18n
import com.replaymod.simplepathing.gui.KeyframeState
import com.replaymod.simplepathing.gui.moveKeyframes
import com.replaymod.simplepathing.gui.moveTimeKeyframes
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.getStringSplitToWidth
import gg.essential.elementa.utils.withAlpha
import java.awt.Color
import kotlin.time.Duration

class UITimePanel(
    val state: KeyframeState,
) : UIBlock(Color(32, 32, 32).withAlpha(0.6f)) {

    val open = BasicState(false)

    val toggleButton by UIButton().constrain {
        width = 9.pixels
        height = 9.pixels
    }.texture(Resources.icon("time_panel")).onMouseClick {
        open.set { !it }
    } as UIButton

    init {
        open.onSetValue { if (it) unhide() else hide(true) }

        constrain {
            width = ChildBasedSizeConstraint() + 10.pixels
            height = ChildBasedSizeConstraint() + 4.pixels
        }
    }

    val container by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf this

    val infoContainer by UIContainer().constrain {
        x = SiblingConstraint(5f)
        y = CenterConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf container

    private val hasSelectedKeyframe = state.selectionTimeKeyframes.map { it.isNotEmpty() }

    val infoCurrentTime by UIText("Current Time".i18n(), shadow = false).constrain {
        color = hasSelectedKeyframe.map { if (it) Color.BLACK else Color.WHITE }.toConstraint()
    } childOf infoContainer

    val infoSelectedKeyframe by UIWrappedText("Selected Time Keyframe".i18n(), shadow = false).constrain {
        y = SiblingConstraint(5f)
        width = basicWidthConstraint { component ->
            component as UIWrappedText
            getStringSplitToWidth(
                component.getText(),
                80f,
                component.getTextScale(),
                ensureSpaceAtEndOfLines = false,
                fontProvider = component.getFontProvider()
            ).maxOf { it.width(component.getTextScale(), component.getFontProvider()) }
        }
        color = hasSelectedKeyframe.map { if (it) Color.WHITE else Color.BLACK }.toConstraint()
    } childOf infoContainer

    val divider by UIBlock(Color.WHITE).constrain {
        x = SiblingConstraint(5f)
        width = 1.pixel
        height = basicHeightConstraint { inputContainer.getHeight() }
    } childOf container

    val inputContainer by UIContainer().constrain {
        x = SiblingConstraint(5f)
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf container

    val replayTimeContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = SiblingConstraint(4f)
        width = ChildBasedSizeConstraint() + 4.pixels // FIXME that constraint is broken and ignores the last padding
        height = ChildBasedMaxSizeConstraint()
    } childOf inputContainer

    val videoTimeContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = SiblingConstraint(4f)
        width = ChildBasedSizeConstraint() + 4.pixels // FIXME that constraint is broken and ignores the last padding
        height = ChildBasedMaxSizeConstraint()
    } childOf inputContainer

    val replayTimeField by UIInputOrExpressionField.forTimeInput().constrain {
        x = 0.pixels(alignOpposite = true)
        width = 60.pixels
        height = 20.pixels
    }.apply {
        input.onActivate {
            val newReplayTime = input.value
            val oldReplayTime = state.selectedTimeKeyframes.get().values.firstOrNull()?.replayTime
            if (oldReplayTime != null) {
                if (oldReplayTime != newReplayTime) {
                    val change = state.moveTimeKeyframes(state.selection.get(), newReplayTime - oldReplayTime)
                    state.mod.currentTimeline.timeline.pushChange(change)
                }
            } else {
                state.gui.replayHandler.doJump(newReplayTime.inWholeMilliseconds.toInt(), true)
            }
        }
    } childOf replayTimeContainer

    val replayTimeLabel by UIText("Replay Time (min:sec:ms)".i18n()).constrain {
        x = SiblingConstraint(5f, alignOpposite = true)
        y = CenterConstraint()
    } childOf replayTimeContainer

    val videoTimeField by UIInputOrExpressionField.forTimeInput().constrain {
        x = 0.pixels(alignOpposite = true)
        width = 60.pixels
        height = 20.pixels
    }.apply {
        input.onActivate {
            val newVideoTime = input.value
            val oldVideoTime = state.selectedTimeKeyframes.get().keys.firstOrNull()
            if (oldVideoTime != null) {
                if (newVideoTime != oldVideoTime) {
                    val timeKeyframes = KeyframeState.Selection.EMPTY.mutate {
                        timeKeyframes += state.selectionTimeKeyframes.get()
                    }
                    val timeline = state.mod.currentTimeline
                    val change = state.moveKeyframes(timeKeyframes, newVideoTime - oldVideoTime)
                    timeline.timeline.pushChange(change)
                }
            } else {
                state.gui.timeline.cursor.position.set(newVideoTime)
            }
        }
    } childOf videoTimeContainer

    val videoTimeLabel by UIText("Video Time (min:sec:ms)".i18n()).constrain {
        x = SiblingConstraint(5f, alignOpposite = true)
        y = CenterConstraint()
    } childOf videoTimeContainer

    init {
        val currentReplayTime =
            pollingState { Duration.milliseconds(state.gui.replayHandler.replaySender.currentTimeStamp()) }
        val currentVideoTime = state.gui.timeline.cursor.position

        state.selectedTimeKeyframes.zip(currentVideoTime.zip(currentReplayTime)).map { (selected, current) ->
            selected.entries.firstOrNull()?.let { (videoTime, keyframe) ->
                videoTime to keyframe.replayTime
            } ?: current
        }.onSetValueAndNow { (videoTime, replayTime) ->
            replayTimeField.input.value = replayTime
            videoTimeField.input.value = videoTime
        }
    }
}