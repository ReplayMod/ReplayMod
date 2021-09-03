package com.replaymod.simplepathing.gui

import com.replaymod.core.gui.common.LazyState
import com.replaymod.core.utils.associateNotNull
import com.replaymod.core.utils.kt
import com.replaymod.core.utils.orNull
import com.replaymod.core.utils.toDouble
import com.replaymod.pathing.properties.CameraProperties
import com.replaymod.pathing.properties.SpectatorProperty
import com.replaymod.pathing.properties.TimestampProperty
import com.replaymod.replaystudio.pathing.change.Change
import com.replaymod.simplepathing.ReplayModSimplePathing
import com.replaymod.simplepathing.SPTimeline
import kotlin.time.Duration

class KeyframeState(
    val mod: ReplayModSimplePathing,
    val gui: GuiPathingKt,
) {
    private var lastTimeline: SPTimeline? = null
    private var lastChange: Change? = null

    val selection = LazyState(Selection(emptySet(), emptySet()))
    val timeKeyframes = LazyState(emptyMap<Duration, TimeKeyframe>())
    val positionKeyframes = LazyState(emptyMap<Duration, PositionKeyframe>())

    val selectionPositionKeyframes = selection.map { it.positionKeyframes }
    val selectionTimeKeyframes = selection.map { it.timeKeyframes }

    val selectedTimeKeyframes = timeKeyframes.zip(selectionTimeKeyframes)
        .map { (keyframes, selection) -> keyframes.filterKeys { it in selection } }
    val selectedPositionKeyframes = positionKeyframes.zip(selectionPositionKeyframes)
        .map { (keyframes, selection) -> keyframes.filterKeys { it in selection } }

    fun update() {
        val selectedTime = setOf(Duration.milliseconds(mod.selectedTime))

        selection.set(Selection(
            if (mod.selectedPath == SPTimeline.SPPath.TIME) selectedTime else emptySet(),
            if (mod.selectedPath == SPTimeline.SPPath.POSITION) selectedTime else emptySet(),
        ))

        val timeline = mod.currentTimeline
        val change = timeline.timeline.peekUndoStack()
        if (timeline != lastTimeline || change != lastChange) {
            lastTimeline = timeline
            lastChange = change

            timeKeyframes.set(timeline.timePath.keyframes.associateNotNull {
                val replayTime = it.getValue(TimestampProperty.PROPERTY).orNull ?: return@associateNotNull null
                Duration.milliseconds(it.time) to TimeKeyframe(Duration.milliseconds(replayTime))
            })
            positionKeyframes.set(timeline.positionPath.keyframes.associateNotNull {
                val position = it.getValue(CameraProperties.POSITION).orNull ?: return@associateNotNull null
                val rotation = it.getValue(CameraProperties.ROTATION).orNull ?: return@associateNotNull null
                val entityId = it.getValue(SpectatorProperty.PROPERTY).orNull
                Duration.milliseconds(it.time) to PositionKeyframe(position.kt, rotation.kt.toDouble(), entityId)
            })
        }

        selection.flush()
        timeKeyframes.flush()
        positionKeyframes.flush()
    }

    fun refreshKeyframes() {
        lastTimeline = null
        lastChange = null
    }

    data class TimeKeyframe(
        val replayTime: Duration,
    )

    data class PositionKeyframe(
        val position: Triple<Double, Double, Double>,
        val rotation: Triple<Double, Double, Double>,
        val entityId: Int?,
    )

    data class Selection(
        val timeKeyframes: Set<Duration>,
        val positionKeyframes: Set<Duration>,
    ) {
        operator fun get(path: SPTimeline.SPPath) = when (path) {
            SPTimeline.SPPath.TIME -> timeKeyframes
            SPTimeline.SPPath.POSITION -> positionKeyframes
        }
    }
}
