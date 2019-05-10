package com.replaymod.extras.youtube;

import net.minecraft.client.resource.language.I18n;

public enum VideoVisibility {
    PUBLIC, UNLISTED, PRIVATE;

    @Override
    public String toString() {
        return I18n.translate("replaymod.gui.videovisibility." + name().toLowerCase());
    }
}
