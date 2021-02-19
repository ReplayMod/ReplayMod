package com.replaymod.core;

import net.minecraftforge.fml.ModList;

import static com.replaymod.core.ReplayMod.MOD_ID;

public class ReplayModBackend {
    public String getVersion() {
        return ModList.get().getModContainerById(MOD_ID).get().getModInfo().getVersion().toString();
    }
}
