package com.replaymod.compat;

import com.replaymod.compat.bettersprinting.DisableBetterSprinting;
import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.compat.oranges17animations.HideInvisibleEntities;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventBus;

@Mod(modid = ReplayModCompat.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        useMetadata = true)
public class ReplayModCompat {
    public static final String MOD_ID = "replaymod-compat";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EventBus bus = FMLCommonHandler.instance().bus();
        bus.register(new DisableFastRender());
        bus.register(new HideInvisibleEntities());
        DisableBetterSprinting.register();
    }

}
