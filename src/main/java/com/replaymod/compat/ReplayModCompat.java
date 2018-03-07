package com.replaymod.compat;

import com.replaymod.compat.bettersprinting.DisableBetterSprinting;
import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.compat.oranges17animations.HideInvisibleEntities;
import org.apache.logging.log4j.Logger;

//#if MC>=10800
import com.replaymod.compat.shaders.ShaderBeginRender;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
//#else
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.event.FMLInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//$$ import cpw.mods.fml.common.eventhandler.EventBus;
//#endif

import static com.replaymod.core.versions.MCVer.*;

@Mod(modid = ReplayModCompat.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        //#endif
        useMetadata = true)
public class ReplayModCompat {
    public static final String MOD_ID = "replaymod-compat";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EventBus bus = FML_BUS;
        //#if MC>=10800
        bus.register(new ShaderBeginRender());
        //#endif
        bus.register(new DisableFastRender());
        bus.register(new HideInvisibleEntities());
        DisableBetterSprinting.register();
    }

}
