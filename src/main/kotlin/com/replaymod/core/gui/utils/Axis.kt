package com.replaymod.core.gui.utils

import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f
import java.awt.Color

enum class Axis(
    val color: Color,
) {
    X(Color.RED),
    Y(Color.GREEN),
    Z(Color.BLUE),
    ;

    fun toVector3f() = when (this) {
        X -> Vector3f(1f, 0f, 0f)
        Y -> Vector3f(0f, 1f, 0f)
        Z -> Vector3f(0f, 0f, 1f)
    }
}
