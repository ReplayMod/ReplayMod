package com.replaymod.editor;

import com.replaymod.core.ReplayMod;
import com.replaymod.editor.handler.GuiHandler;
import com.replaymod.online.Setting;
import org.apache.logging.log4j.Logger;

//#if MC>=10800
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
//#else
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.event.FMLInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//#endif

@Mod(modid = ReplayModEditor.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        //#endif
        useMetadata = true)
public class ReplayModEditor {
    public static final String MOD_ID = "replaymod-editor";

    @Mod.Instance(MOD_ID)
    public static ReplayModEditor instance;

    private ReplayMod core;

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ReplayModEditor.LOGGER = event.getModLog();
        core = ReplayMod.instance;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        new GuiHandler(this).register();
    }

    public ReplayMod getCore() {
        return core;
    }
}
