package com.replaymod.render;

import com.replaymod.core.ReplayMod;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ReplayModRender.MOD_ID, useMetadata = true)
public class ReplayModRender {
    public static final String MOD_ID = "replaymod-render";

    @Mod.Instance(MOD_ID)
    public static ReplayModRender instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private Logger logger;

    private Configuration configuration;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        configuration = new Configuration(event.getSuggestedConfigurationFile());
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
