package com.replaymod.extras.youtube;

import net.minecraft.client.resources.I18n;

public enum VideoVisibility {
    PUBLIC, UNLISTED, PRIVATE;

    @Override
    public String toString() {
        return I18n.format("replaymod.gui.videovisibility." + name().toLowerCase());
    }
}
