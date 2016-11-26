package com.replaymod.render;

import com.replaymod.core.ReplayMod;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

@Mod(modid = ReplayModRender.MOD_ID, useMetadata = true)
public class ReplayModRender {
    public static final String MOD_ID = "replaymod-render";

    @Mod.Instance(MOD_ID)
    public static ReplayModRender instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private Logger logger;

    private Configuration configuration;

    public ReplayMod getCore() {
        return core;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        configuration = new Configuration(event.getSuggestedConfigurationFile());

        core.getSettingsRegistry().register(Setting.class);
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File folder = new File(path.startsWith("./") ? core.getMinecraft().mcDataDir : null, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
