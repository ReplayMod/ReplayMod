package com.replaymod.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import static com.replaymod.core.ReplayMod.MOD_ID;

public class ReplayModBackend {
    public String getVersion() {
        return ModList.get().getModContainerById(MOD_ID).get().getModInfo().getVersion().toString();
    }

    public String getMinecraftVersion() {
        return Minecraft.getInstance().getMinecraftGame().getVersion().getName();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id.toLowerCase());
    }
}
