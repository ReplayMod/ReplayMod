package com.replaymod.render;

import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.events.ReplayCloseEvent;
import net.minecraft.crash.CrashReport;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=10800
import net.minecraftforge.fml.common.Mod;
//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.*;

public class ReplayModRender implements Module {
    { instance = this; }
    public static ReplayModRender instance;

    private ReplayMod core;

    public static Logger LOGGER = LogManager.getLogger();

    private final List<RenderJob> renderQueue = new ArrayList<>();

    public ReplayModRender(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    public ReplayMod getCore() {
        return core;
    }

    @Override
    public void initClient() {
        FML_BUS.register(this);
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Post event) {
        renderQueue.clear();
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File folder = new File(path.startsWith("./") ? mcDataDir(core.getMinecraft()) : null, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw newReportedException(CrashReport.makeCrashReport(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Path getRenderSettingsPath() {
        return mcDataDir(core.getMinecraft()).toPath().resolve("config/replaymod-rendersettings.json");
    }

    public List<RenderJob> getRenderQueue() {
        return renderQueue;
    }
}
