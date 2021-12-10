package com.replaymod.core.gui.utils

import com.replaymod.core.ReplayMod
import net.minecraft.util.Identifier

object Resources {
    private const val namespace = ReplayMod.MOD_ID

    fun icon(name: String) = Identifier(namespace, "icons/$name.png")
}
