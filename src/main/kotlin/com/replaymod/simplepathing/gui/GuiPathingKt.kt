package com.replaymod.simplepathing.gui

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.replaymod.core.ReplayMod
import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.common.UITexture
import com.replaymod.core.gui.common.scrollbar.UITexturedScrollBar
import com.replaymod.core.gui.common.timeline.UITimeline
import com.replaymod.core.gui.common.timeline.UITimelineTime
import com.replaymod.core.gui.utils.*
import com.replaymod.core.utils.*
import com.replaymod.core.versions.MCVer.Keyboard
import com.replaymod.pathing.player.RealtimeTimelinePlayer
import com.replaymod.render.gui.GuiRenderQueue
import com.replaymod.render.gui.GuiRenderSettings
import com.replaymod.replay.ReplayHandler
import com.replaymod.simplepathing.SPTimeline
import com.replaymod.simplepathing.SPTimeline.SPPath
import com.replaymod.simplepathing.Setting
import de.johni0702.minecraft.gui.popup.GuiInfoPopup
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import java.util.concurrent.CancellationException
import kotlin.time.Duration

class GuiPathingKt(
    val java: GuiPathing,
    val replayHandler: ReplayHandler,
) {
    private val state = KeyframeState(java.mod, this)
    private val overlay = replayHandler.overlay
    private val mod = java.mod
    private val core = mod.core
    private val window = overlay.kt.window
    private val player = RealtimeTimelinePlayer(replayHandler)

    private val isCtrlDown = window.pollingState { Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) }
    private val isShiftDown = window.pollingState { Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) }

    private val playing = window.pollingState { player.isActive }

    init {
        window.onAnimationFrame { state.update() }
    }

    private val secondRow by UIContainer().constrain {
        x = CenterConstraint()
        y = 30.pixels /* topPanel.bottom */ + 13.pixels
        width = 100.percent - 20.pixels
        height = 20.pixels
    } childOf window

    private val playPauseButton by UIButton().constrain {
        width = 20.pixels
        height = 20.pixels
    }.texture(ReplayMod.TEXTURE, playing.map {
        UITexture.TextureData.ofSize(0, if (it) 20 else 0, 20, 20)
    }).addTooltip {
        addLine {
            bindText(playing.zip(isCtrlDown).map { (playing, isCtrlDown) ->
                when {
                    playing -> "replaymod.gui.ingame.menu.pausepath"
                    isCtrlDown -> "replaymod.gui.ingame.menu.playpathfromstart"
                    else -> "replaymod.gui.ingame.menu.playpath"
                }.i18n()
            })
        }
    }.onMouseClick {
        if (player.isActive) {
            player.future.cancel(false)
        } else {
            val ignoreTimeKeyframes = isShiftDown.get()

            val timeline = java.preparePathsForPlayback(ignoreTimeKeyframes).okOrElse { err ->
                GuiInfoPopup.open(overlay, *err)
                null
            } ?: return@onMouseClick

            val timePath = SPTimeline(timeline).timePath
            timePath.isActive = !ignoreTimeKeyframes

            // Start from cursor time unless the control key is pressed (then start from beginning)
            val startTime = if (isCtrlDown.get()) Duration.ZERO else this@GuiPathingKt.timeline.cursor.position.get()
            val future: ListenableFuture<Void> = player.start(timeline, startTime.inWholeMilliseconds)
            overlay.isCloseable = false
            overlay.isMouseVisible = true
            core.printInfoToChat("replaymod.chat.pathstarted")
            Futures.addCallback(future, object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    if (future.isCancelled) {
                        core.printInfoToChat("replaymod.chat.pathinterrupted")
                    } else {
                        core.printInfoToChat("replaymod.chat.pathfinished")
                    }
                    overlay.isCloseable = true
                }

                override fun onFailure(t: Throwable) {
                    if (t !is CancellationException) {
                        t.printStackTrace()
                    }
                    overlay.isCloseable = true
                }
            })
        }
    } childOf secondRow

    private val renderButton by UIButton().constrain {
        x = SiblingConstraint(5f)
        width = 20.pixels
        height = 20.pixels
    }.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(40, 0, 20, 20)) {
    }.addTooltip {
        addLine("replaymod.gui.ingame.menu.renderpath".i18n())
    }.onMouseClick {
        abortPathPlayback()
        val screen = GuiRenderSettings.createBaseScreen()
        object : GuiRenderQueue(screen, replayHandler, { java.preparePathsForPlayback(false) }) {
            override fun close() {
                super.close()
                minecraft.openScreen(null)
            }
        }.open()
        screen.display()
    } childOf secondRow

    private data class PositionButtonType(
        val type: KeyframeType = KeyframeType.POSITION,
        val remove: Boolean = false,
    )

    private val positionKeyframeButtonType = window.pollingState(PositionButtonType()) {
        val time = state.selectedPositionKeyframes.get().keys.firstOrNull()
            ?: timeline.cursor.position.get()
        val keyframe = state.positionKeyframes.get()[time]
        PositionButtonType(when {
            keyframe?.entityId != null -> KeyframeType.SPECTATOR
            keyframe == null && !replayHandler.isCameraView -> KeyframeType.SPECTATOR
            else -> KeyframeType.POSITION
        }, keyframe != null)
    }

    private val positionKeyframeButton by UIButton().constrain {
        x = SiblingConstraint(5f)
        width = 20.pixels
        height = 20.pixels
    }.texture(ReplayMod.TEXTURE, positionKeyframeButtonType.map { (type, remove) ->
        type.buttonIcon.offset(0, if (remove) 20 else 0)
    }).addTooltip {
        addLine {
            bindText(positionKeyframeButtonType.map { (type, remove) ->
                type.tooltip(!remove).i18n() + " (" + mod.keyPositionKeyframe.boundKey + ")"
            })
        }
    }.onMouseClick {
        java.toggleKeyframe(SPPath.POSITION, false)
    } childOf secondRow

    private val timeKeyframePresent: State<Boolean> = window.pollingState(false) {
        val time = state.selectedTimeKeyframes.get().keys.firstOrNull()
            ?: timeline.cursor.position.get()
        val keyframe = state.timeKeyframes.get()[time]
        keyframe != null
    }

    private val timeKeyframeButton by UIButton().constrain {
        x = SiblingConstraint(5f)
        width = 20.pixels
        height = 20.pixels
    }.texture(ReplayMod.TEXTURE, timeKeyframePresent.map { present ->
        KeyframeType.TIME.buttonIcon.offset(0, if (present) 20 else 0)
    }).addTooltip {
        addLine {
            bindText(timeKeyframePresent.map { present ->
                KeyframeType.TIME.tooltip(!present).i18n() + " (" + mod.keyTimeKeyframe.boundKey + ")"
            })
        }
    }.onMouseClick {
        java.toggleKeyframe(SPPath.TIME, false)
    } childOf secondRow

    val timeline by UITimeline().constrain {
        x = SiblingConstraint(5f)
        width = FillConstraint(false) - basicWidthConstraint { zoomButtonPanel.getWidth() } - 2.pixels
        height = 20.pixels
    }.apply {
        enableIndicators()
        enableZooming()

        zoom.set(0.1f)
        length.set(Duration.seconds(core.settingsRegistry.get(Setting.TIMELINE_LENGTH)))

        content.insertChildBefore(UITimelineKeyframes(state, overlay.timeline), cursor)
    }.onLeftMouse { mouseX, _ ->
        val time = getTimeAt(mouseX)
        cursor.position.set(time)
        mod.setSelected(null, 0)
    }.onLeftClick {
        it.stopImmediatePropagation()
    }.onAnimationFrame {
        if (player.isActive) {
            cursor.position.set(Duration.milliseconds(player.timePassed))
            cursor.ensureVisibleWithPadding()
        }
    } childOf secondRow

    private val scrollbar by UITexturedScrollBar().constrain {
        x = 0.pixels boundTo timeline
        y = SiblingConstraint(1f) boundTo timeline
        width = CopyConstraintFloat() boundTo timeline
        height = 9.pixels
    }.apply {
        timeline.content.setHorizontalScrollBarComponent(grip)
    } childOf window

    private val timelineTime by UITimelineTime(timeline).constrain {
        x = 0.pixels boundTo timeline
        y = SiblingConstraint(alignOpposite = true) boundTo timeline
        width = CopyConstraintFloat() boundTo timeline
        height = 8.pixels
    } childOf window

    private val zoomButtonPanel by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf secondRow

    private val zoomInButton by UIButton().constrain {
        width = 9.pixels
        height = 9.pixels
    }.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(40, 20, 9, 9)) {
    }.addTooltip {
        addLine("replaymod.gui.ingame.menu.zoomin".i18n())
    }.onMouseClick {
        timeline.zoom.set { it * 2 / 3 }
    } childOf zoomButtonPanel

    private val zoomOutButton by UIButton().constrain {
        y = SiblingConstraint(2f)
        width = 9.pixels
        height = 9.pixels
    }.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(40, 30, 9, 9)) {
    }.addTooltip {
        addLine("replaymod.gui.ingame.menu.zoomout".i18n())
    }.onMouseClick {
        timeline.zoom.set { it * 3 / 2 }
    } childOf zoomButtonPanel

    init {
        val speedValue = window.pollingState { overlay.speedSlider.value }
        val replayTime = window.pollingState { replayHandler.replaySender.currentTimeStamp() }
        speedValue.zip(replayTime).onSetValue {
            if (mod.keySyncTime.isAutoActivating && !player.isActive) {
                syncTimeButtonPressed()
            }
        }
    }

    fun abortPathPlayback() {
        if (!player.isActive) {
            return
        }
        val future = player.future
        if (!future.isDone && !future.isCancelled) {
            future.cancel(false)
        }
        // Tear down of the player might only happen the next tick after it was cancelled
        player.onTick()
    }

    fun syncTimeButtonPressed() {
        // Current replay time
        val currentReplayTime = Duration.milliseconds(replayHandler.replaySender.currentTimeStamp())
        // Position of the cursor
        val cursor = timeline.cursor.position.get()
        // Get the last time keyframe before the cursor
        val (keyframeCursor, keyframe) = state.timeKeyframes.get().entries.findLast { (time, _) -> time <= cursor }
            ?: return
        val keyframeReplayTime = keyframe.replayTime
        // Replay time passed
        val replayTimePassed = currentReplayTime - keyframeReplayTime
        // Speed (set to 1 when shift is held)
        val speed = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) 1.0 else overlay.speedSliderValue
        // Cursor time passed
        val cursorPassed = replayTimePassed / speed
        // Move cursor to new position
        timeline.cursor.position.set(keyframeCursor + cursorPassed)
        timeline.cursor.ensureVisibleWithPadding()
        // Deselect keyframe to allow the user to add a new one right away
        mod.setSelected(null, 0)
    }
}