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
import com.replaymod.core.utils.i18n
import com.replaymod.core.versions.MCVer.Keyboard
import com.replaymod.pathing.player.RealtimeTimelinePlayer
import com.replaymod.render.gui.GuiRenderQueue
import com.replaymod.render.gui.GuiRenderSettings
import com.replaymod.replay.ReplayHandler
import com.replaymod.replaystudio.pathing.change.CombinedChange
import com.replaymod.simplepathing.SPTimeline
import com.replaymod.simplepathing.SPTimeline.SPPath
import com.replaymod.simplepathing.Setting
import com.replaymod.simplepathing.gui.panels.UIPositionKeyframePanel
import com.replaymod.simplepathing.gui.panels.UIPositionOffsetPanel
import com.replaymod.simplepathing.gui.panels.UITimeOffsetPanel
import com.replaymod.simplepathing.gui.panels.UITimePanel
import de.johni0702.minecraft.gui.popup.GuiInfoPopup
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import net.minecraft.client.resource.language.I18n
import java.util.concurrent.CancellationException
import kotlin.time.Duration

class GuiPathingKt(
    val java: GuiPathing,
    val replayHandler: ReplayHandler,
) {
    val state = KeyframeState(java.mod, this)
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

        window.onBoxSelection { components, _ ->
            state.selection.set(KeyframeState.Selection.EMPTY.mutate {
                for (keyframe in components.filterIsInstance<UITimelineKeyframes.UIKeyframe>()) {
                    this[keyframe.type.path] += keyframe.time
                }
            })
        }
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
        val keyframe = state.selectedPositionKeyframes.get().values.firstOrNull()
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
        toggleKeyframe(positionKeyframeButtonType.get().type)
    } childOf secondRow

    private val timeKeyframePresent: State<Boolean> = window.pollingState(false) {
        val keyframe = state.selectedTimeKeyframes.get().values.firstOrNull()
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
        toggleKeyframe(KeyframeType.TIME)
    } childOf secondRow

    private val unusedButton by UIContainer().constrain {
        x = SiblingConstraint(5f)
        width = 25.pixels
        height = 20.pixels
    } childOf secondRow

    val timeline by UITimeline().constrain {
        x = SiblingConstraint(5f)
        width = FillConstraint(false)
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
        state.selection.set(KeyframeState.Selection.EMPTY)
    }.onLeftClick {
        it.stopImmediatePropagation()
    }.onAnimationFrame {
        if (player.isActive) {
            cursor.position.set(Duration.milliseconds(player.timePassed))
            cursor.ensureVisibleWithPadding()
        }
    } childOf secondRow

    private val belowTimeline by UIContainer().constrain {
        x = 0.pixels boundTo timeline
        y = SiblingConstraint(1f) boundTo timeline
        width = CopyConstraintFloat() boundTo timeline
        height = 9.pixels
    } childOf window

    private val belowTimelineButtons by UIContainer().constrain {
        width = ChildBasedSizeConstraint()
        height = 100.percent
    } childOf belowTimeline

    private val scrollbar by UITexturedScrollBar().constrain {
        x = SiblingConstraint(2f)
        width = FillConstraint(useSiblings = false)
        height = 100.percent
    }.apply {
        timeline.content.setHorizontalScrollBarComponent(grip)
    } childOf belowTimeline

    private val timelineTime by UITimelineTime(timeline).constrain {
        x = 0.pixels boundTo timeline
        y = SiblingConstraint(alignOpposite = true) boundTo timeline
        width = CopyConstraintFloat() boundTo timeline
        height = 8.pixels
    } childOf window

    private val zoomInButton by UIButton().constrain {
        x = SiblingConstraint(2f)
        width = 9.pixels
        height = 9.pixels
    }.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(40, 20, 9, 9)) {
    }.addTooltip {
        addLine("replaymod.gui.ingame.menu.zoomin".i18n())
    }.onMouseClick {
        timeline.zoom.set { it * 2 / 3 }
    } childOf belowTimelineButtons

    private val zoomOutButton by UIButton().constrain {
        x = SiblingConstraint(2f)
        width = 9.pixels
        height = 9.pixels
    }.texture(ReplayMod.TEXTURE, UITexture.TextureData.ofSize(40, 30, 9, 9)) {
    }.addTooltip {
        addLine("replaymod.gui.ingame.menu.zoomout".i18n())
    }.onMouseClick {
        timeline.zoom.set { it * 3 / 2 }
    } childOf belowTimelineButtons

    private val positionKeyframePanel by UIPositionKeyframePanel(state).apply {
        overlay.kt.bottomRightPanel.insertChildAt(toggleButton, 0)
    } hiddenChildOf window

    private val timePanel by UITimePanel(state).constrain {
        x = 0.pixels boundTo belowTimeline
        y = SiblingConstraint(1f) boundTo belowTimeline
    }.apply {
        belowTimelineButtons.insertChildAt(toggleButton.constrain {
            x = SiblingConstraint(2f)
        }, 0)
    } hiddenChildOf window

    private val timeOffsetPanel by UITimeOffsetPanel(state, timePanel).constrain {
        x = 0.pixels boundTo secondRow
        y = SiblingConstraint(1f) boundTo belowTimeline
        width = basicWidthConstraint { timePanel.getLeft() - 5f - it.getLeft() }
    } hiddenChildOf window

    val positionOffsetPanel by UIPositionOffsetPanel(window, state).apply {
        toggleButton.constrain {
            x = SiblingConstraint(4f)
            y = 0.pixels(alignOpposite = true)
        }
        overlay.kt.bottomLeftPanel.insertChildAt(toggleButton, 0)
    } hiddenChildOf window

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

    private fun computeSyncTime(cursor: Duration): Duration? {
        // Current replay time
        val currentReplayTime = Duration.milliseconds(replayHandler.replaySender.currentTimeStamp())
        // Get the last time keyframe before the cursor
        val (keyframeCursor, keyframe) = state.timeKeyframes.get().entries.findLast { (time, _) -> time <= cursor }
            ?: return null
        val keyframeReplayTime = keyframe.replayTime
        // Replay time passed
        val replayTimePassed = currentReplayTime - keyframeReplayTime
        // Speed (set to 1 when shift is held)
        val speed = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) 1.0 else overlay.speedSliderValue
        // Cursor time passed
        val cursorPassed = replayTimePassed / speed
        // Return new position
        return keyframeCursor + cursorPassed
    }

    fun syncTimeButtonPressed() {
        // Position of the cursor
        var cursor = timeline.cursor.position.get()


        // Update cursor once
        cursor = computeSyncTime(cursor)
            ?: return  // no keyframes before cursor, nothing we can do

        // Repeatedly update until we find a fix point
        while (true) {
            // If the cursor has gotten stuck before in front of all keyframes,
            // let's just use the last value we got, this shouldn't happen with ordinary timelines anyway.
            val updatedCursor = computeSyncTime(cursor) ?: break

            if (updatedCursor == cursor) {
                // Found the fix point, we can stop now
                break
            }
            if (updatedCursor < cursor) {
                // We've gone backwards, we'll likely get stuck in a loop, so abort the whole thing
                return
            }
            // Found a new position, take it, repeat
            cursor = updatedCursor
        }

        // Move cursor to new position
        timeline.cursor.position.set(cursor)
        timeline.cursor.ensureVisibleWithPadding()
        // Deselect keyframe to allow the user to add a new one right away
        state.selection.set(KeyframeState.Selection.EMPTY)
    }

    @JvmOverloads
    fun toggleKeyframe(type: KeyframeType, neverSpectator: Boolean = false) {
        val path = type.path
        val time = timeline.cursor.position.get()
        val timeline = mod.currentTimeline

        if (state.keyframes.values.all { it.get().isEmpty() } && time > Duration.seconds(1)) {
            val text = I18n.translate("replaymod.gui.ingame.first_keyframe_not_at_start_warning")
            GuiInfoPopup.open(overlay, *text.split("\\n").toTypedArray())
        }

        val selection = state.selection.get()[path]
        if (selection.isEmpty()) {
            // Nothing selected, create new keyframe

            // If a keyframe is already present at this time, cannot add another one
            if (time in state[path].get()) {
                return
            }

            when (path) {
                SPPath.TIME -> {
                    timeline.addTimeKeyframe(time.inWholeMilliseconds, replayHandler.replaySender.currentTimeStamp())
                }
                SPPath.POSITION -> {
                    val camera = replayHandler.cameraEntity
                    var spectatedId = -1
                    if (!replayHandler.isCameraView && !neverSpectator) {
                        spectatedId = replayHandler.overlay.minecraft.getCameraEntity()!!.entityId
                    }
                    timeline.addPositionKeyframe(time.inWholeMilliseconds,
                        camera.x, camera.y, camera.z,
                        camera.yaw, camera.pitch, camera.roll,
                        spectatedId)
                }
            }
        } else {
            // Keyframe(s) selected, remove them
            state.selection.set { it.mutate { this[path].clear() } }
            val changes = selection.map { timeline.removeKeyframe(path, it.inWholeMilliseconds) }
            timeline.timeline.pushChange(CombinedChange.createFromApplied(*changes.toTypedArray()))
        }

        state.update()
    }

    fun deleteButtonPressed(): Boolean {
        val timeline = mod.currentTimeline ?: return false

        val selection = state.selection.get().toMap()
        if (selection.all { it.value.isEmpty() }) {
            return false
        }

        state.selection.set(KeyframeState.Selection.EMPTY)
        val changes = selection.flatMap { (path, keyframes) ->
            keyframes.map { timeline.removeKeyframe(path, it.inWholeMilliseconds) }
        }
        timeline.timeline.pushChange(CombinedChange.createFromApplied(*changes.toTypedArray()))

        return true
    }
}