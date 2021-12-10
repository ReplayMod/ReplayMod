package com.replaymod.simplepathing.gui

import com.replaymod.core.utils.kt
import com.replaymod.core.utils.orNull
import com.replaymod.pathing.properties.CameraProperties
import com.replaymod.pathing.properties.SpectatorProperty
import com.replaymod.pathing.properties.TimestampProperty
import com.replaymod.replaystudio.pathing.change.Change
import com.replaymod.replaystudio.pathing.change.CombinedChange
import com.replaymod.simplepathing.SPTimeline
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f
import kotlin.time.Duration

fun KeyframeState.moveKeyframes(keyframes: KeyframeState.Selection, deltaTime: Duration): Change {
    if (deltaTime == Duration.ZERO) {
        return CombinedChange.createFromApplied()
    }

    val spTimeline = mod.currentTimeline ?: throw IllegalStateException()

    val changes = mutableListOf<Change>()
    val newSelection = keyframes.mapWithPath(reverse = deltaTime.isPositive()) { path, oldTime ->
        var newTime = (oldTime + deltaTime).coerceAtLeast(Duration.ZERO)

        // If there already is a keyframe at the target time, then increase the time by one until there is none
        while (spTimeline.getKeyframe(path, newTime.inWholeMilliseconds) != null) {
            newTime += Duration.milliseconds(1)
        }

        changes += spTimeline.moveKeyframe(path, oldTime.inWholeMilliseconds, newTime.inWholeMilliseconds)

        newTime
    }

    this.selection.set(newSelection)

    return CombinedChange.createFromApplied(*changes.toTypedArray())
}

fun KeyframeState.movePosKeyframes(keyframes: KeyframeState.Selection, deltaPos: Vector3f, deltaRot: Vector3f): Change {
    val spTimeline = mod.currentTimeline ?: throw IllegalStateException()

    val changes = mutableListOf<Change>()
    for (time in keyframes[SPTimeline.SPPath.POSITION]) {
        val keyframe = spTimeline.getKeyframe(SPTimeline.SPPath.POSITION, time.inWholeMilliseconds) ?: continue
        if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent) continue
        val (x, y, z) = keyframe.getValue(CameraProperties.POSITION).orNull?.kt ?: continue
        val (yaw, pitch, roll) = keyframe.getValue(CameraProperties.ROTATION).orNull?.kt ?: continue
        changes += spTimeline.updatePositionKeyframe(
            time.inWholeMilliseconds,
            x + deltaPos.x, y + deltaPos.y, z + deltaPos.z,
            yaw + deltaRot.x, pitch + deltaRot.y, roll + deltaRot.z
        )
    }

    return CombinedChange.createFromApplied(*changes.toTypedArray())
}

fun KeyframeState.moveTimeKeyframes(keyframes: KeyframeState.Selection, deltaTime: Duration): Change {
    val spTimeline = mod.currentTimeline ?: throw IllegalStateException()

    val changes = mutableListOf<Change>()
    for (time in keyframes[SPTimeline.SPPath.TIME]) {
        val keyframe = spTimeline.getKeyframe(SPTimeline.SPPath.TIME, time.inWholeMilliseconds) ?: continue
        val replayTime = keyframe.getValue(TimestampProperty.PROPERTY).orNull ?: continue
        val newReplayTime = (replayTime + deltaTime.inWholeMilliseconds).coerceAtLeast(0)
        changes += spTimeline.updateTimeKeyframe(time.inWholeMilliseconds, newReplayTime.toInt())
    }

    return CombinedChange.createFromApplied(*changes.toTypedArray())
}
