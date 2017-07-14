package com.replaymod.compat;

import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.compat.shaders.ShaderBeginRender;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;

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
        bus.register(new ShaderBeginRender());
        bus.register(new DisableFastRender());
    }

}
