package com.replaymod.simplepathing.gui

import com.replaymod.core.gui.common.lazy
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
import gg.essential.elementa.state.BasicState
import kotlin.time.Duration

class KeyframeState(
    val mod: ReplayModSimplePathing,
    val gui: GuiPathingKt,
) {
    private var lastTimeline: SPTimeline? = null
    private var lastChange: Change? = null

    val selection = BasicState(Selection.EMPTY).lazy()
    val timeKeyframes = BasicState(emptyMap<Duration, TimeKeyframe>()).lazy()
    val positionKeyframes = BasicState(emptyMap<Duration, PositionKeyframe>()).lazy()

    operator fun get(path: SPTimeline.SPPath) = when (path) {
        SPTimeline.SPPath.TIME -> timeKeyframes
        SPTimeline.SPPath.POSITION -> positionKeyframes
    }
    val keyframes = SPTimeline.SPPath.values().associateWith { get(it) }

    val selectionPositionKeyframes = selection.map { it.positionKeyframes }
    val selectionTimeKeyframes = selection.map { it.timeKeyframes }

    val selectedTimeKeyframes = timeKeyframes.zip(selectionTimeKeyframes)
        .map { (keyframes, selection) -> keyframes.filterKeys { it in selection } }
    val selectedPositionKeyframes = positionKeyframes.zip(selectionPositionKeyframes)
        .map { (keyframes, selection) -> keyframes.filterKeys { it in selection } }

    init {
        selection.inner.onSetValue {
            if (it != Selection.EMPTY) {
                gui.java.overlay.timeline.selectedMarker = null
            }
        }
    }

    fun update() {
        if (gui.java.overlay.timeline.selectedMarker != null) {
            selection.set(Selection.EMPTY)
        }

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

    @Deprecated("does not support multi-selection")
    fun getSelectedPath(): SPTimeline.SPPath? {
        val selection = selection.get()
        return when {
            selection.timeKeyframes.isNotEmpty() -> SPTimeline.SPPath.TIME
            selection.positionKeyframes.isNotEmpty() -> SPTimeline.SPPath.POSITION
            else -> null
        }
    }

    @Deprecated("does not support multi-selection")
    fun getSelectedTime(): Long {
        val selection = selection.get()
        val time = selection.timeKeyframes.firstOrNull() ?: selection.positionKeyframes.firstOrNull() ?: Duration.ZERO
        return time.inWholeMilliseconds
    }

    @Deprecated("does not support multi-selection")
    fun setSelected(path: SPTimeline.SPPath?, time: Long) {
        val keyframeSet = mutableSetOf(Duration.milliseconds(time))
        selection.set(when (path) {
            SPTimeline.SPPath.TIME -> Selection.EMPTY.mutate { timeKeyframes = keyframeSet }
            SPTimeline.SPPath.POSITION -> Selection.EMPTY.mutate { positionKeyframes = keyframeSet }
            null -> Selection.EMPTY
        })
    }

    fun isSelected(path: SPTimeline.SPPath, time: Long): Boolean =
        Duration.milliseconds(time) in selection.get()[path]

    data class TimeKeyframe(
        val replayTime: Duration,
    )

    data class PositionKeyframe(
        val position: Triple<Double, Double, Double>,
        val rotation: Triple<Double, Double, Double>,
        val entityId: Int?,
    )

    class Selection(
        timeKeyframes: Set<Duration>,
        positionKeyframes: Set<Duration>,
    ) {
        val timeKeyframes: Set<Duration> = timeKeyframes.toSortedSet()
        val positionKeyframes: Set<Duration> = positionKeyframes.toSortedSet()

        constructor(paths: Map<SPTimeline.SPPath, Set<Duration>>) : this(
            paths[SPTimeline.SPPath.TIME] ?: emptySet(),
            paths[SPTimeline.SPPath.POSITION] ?: emptySet(),
        )

        fun toMap() = SPTimeline.SPPath.values().associateWith { get(it) }

        val size: Int
            get() = toMap().values.sumOf { it.size }

        operator fun get(path: SPTimeline.SPPath) = when (path) {
            SPTimeline.SPPath.TIME -> timeKeyframes
            SPTimeline.SPPath.POSITION -> positionKeyframes
        }

        inline fun mutate(block: MutableSelection.() -> Unit): Selection =
            MutableSelection(this).apply(block).toSelection()

        inline fun map(func: (Duration) -> Duration): Selection =
            mapWithPath { _, duration -> func(duration) }

        inline fun mapWithPath(
            reverse: Boolean = false,
            func: (SPTimeline.SPPath, Duration) -> Duration,
        ): Selection = Selection(toMap().mapValues { (path, keyframes) ->
            if (!reverse) {
                keyframes.mapTo(mutableSetOf()) { func(path, it) }
            } else {
                keyframes.reversed().map { func(path, it) }.asReversed().toSet()
            }
        })

        companion object {
            fun single(path: SPTimeline.SPPath, time: Duration): Selection = Selection(mapOf(path to setOf(time)))

            @JvmField
            val EMPTY = Selection(emptySet(), emptySet())
        }
    }

    class MutableSelection(
        var timeKeyframes: MutableSet<Duration>,
        var positionKeyframes: MutableSet<Duration>,
    ) {
        constructor(selection: Selection) : this(
            selection.timeKeyframes.toMutableSet(),
            selection.positionKeyframes.toMutableSet(),
        )

        fun toSelection() = Selection(
            timeKeyframes.toSet(),
            positionKeyframes.toSet(),
        )

        operator fun get(path: SPTimeline.SPPath) = when (path) {
            SPTimeline.SPPath.TIME -> timeKeyframes
            SPTimeline.SPPath.POSITION -> positionKeyframes
        }
    }
}
