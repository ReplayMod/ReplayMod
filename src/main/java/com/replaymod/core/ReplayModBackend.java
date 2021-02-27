package com.replaymod.core;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import static com.replaymod.core.ReplayMod.MOD_ID;

public class ReplayModBackend implements ClientModInitializer {
    private final ReplayMod mod = new ReplayMod(this);

    @Override
    public void onInitializeClient() {
        mod.initModules();
    }

    public String getVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(IllegalStateException::new)
                .getMetadata().getVersion().toString();
    }

    public String getMinecraftVersion() {
        return mod.getMinecraft().getGame().getVersion().getName();
    }

    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id.toLowerCase());
    }
}
