package com.replaymod.render;

import com.replaymod.core.ReplayMod;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.events.ReplayCloseEvent;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = ReplayModRender.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        useMetadata = true)
public class ReplayModRender {
    public static final String MOD_ID = "replaymod-render";

    @Mod.Instance(MOD_ID)
    public static ReplayModRender instance;

    private ReplayMod core;

    public static Logger LOGGER;

    private Configuration configuration;
    private final List<RenderJob> renderQueue = new ArrayList<>();

    public ReplayMod getCore() {
        return core;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        core = ReplayMod.instance;
        configuration = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(this);

        core.getSettingsRegistry().register(Setting.class);
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Post event) {
        renderQueue.clear();
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

    public List<RenderJob> getRenderQueue() {
        return renderQueue;
    }
}
