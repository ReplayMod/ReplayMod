package com.replaymod.simplepathing.gui

import com.replaymod.core.gui.common.UITexture
import com.replaymod.simplepathing.SPTimeline

private val buttonIcon = UITexture.TextureData.ofSize(20, 20)
private val timelineIcon = UITexture.TextureData.ofSize(5, 5)

enum class KeyframeType(
    val path: SPTimeline.SPPath,
    val buttonIcon: UITexture.TextureData,
    val timelineIcon: UITexture.TextureData,
    val tooltipAdd: String,
    val tooltipRemove: String,
) {
    TIME(
        SPTimeline.SPPath.TIME,
        buttonIcon.offset(0, 80),
        timelineIcon.offset(74, 25),
        "replaymod.gui.ingame.menu.addtimekeyframe",
        "replaymod.gui.ingame.menu.removetimekeyframe",
    ),
    POSITION(
        SPTimeline.SPPath.POSITION,
        buttonIcon.offset(0, 40),
        timelineIcon.offset(74, 20),
        "replaymod.gui.ingame.menu.addposkeyframe",
        "replaymod.gui.ingame.menu.removeposkeyframe",
    ),
    SPECTATOR(
        SPTimeline.SPPath.POSITION,
        buttonIcon.offset(40, 40),
        timelineIcon.offset(74, 30),
        "replaymod.gui.ingame.menu.addspeckeyframe",
        "replaymod.gui.ingame.menu.removespeckeyframe",
    ),
    ;

    fun tooltip(add: Boolean) = if (add) tooltipAdd else tooltipRemove
}
