package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(modid = ReplayModExtras.MOD_ID, useMetadata = true)
public class ReplayModExtras {
    public static final String MOD_ID = "replaymod-extras";

    @Mod.Instance(MOD_ID)
    public static ReplayModExtras instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private static final List<Class<? extends Extra>> builtin = Arrays.asList(
            FullBrightness.class,
            HotkeyButtons.class
    );

    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        for (Class<? extends Extra> cls : builtin) {
            try {
                Extra extra = cls.newInstance();
                extra.register(core);
            } catch (Throwable t) {
                logger.warn("Failed to load extra " + cls.getName() + ": ", t);
            }
        }
    }
}
