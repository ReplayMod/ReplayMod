package com.replaymod.online;

import com.replaymod.core.ReplayMod;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.gui.GuiLoginPrompt;
import com.replaymod.online.gui.GuiReplayDownloading;
import com.replaymod.online.handler.GuiHandler;
import com.replaymod.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

@Mod(modid = ReplayModOnline.MOD_ID, useMetadata = true)
public class ReplayModOnline {
    public static final String MOD_ID = "replaymod-online";

    @Mod.Instance(MOD_ID)
    public static ReplayModOnline instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    @Mod.Instance(ReplayModReplay.MOD_ID)
    private static ReplayModReplay replayModule;

    private Logger logger;

    private File downloadsFolder = new File("replay_downloads");

    private ApiClient apiClient;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        core.getSettingsRegistry().register(Setting.class);

        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        ConfigurationAuthData authData = new ConfigurationAuthData(config);
        apiClient = new ApiClient(authData);
        authData.load(apiClient);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!downloadsFolder.mkdirs()) {
            logger.warn("Failed to create downloads folder: " + downloadsFolder);
        }

        new GuiHandler(this).register();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Initial login prompt
        if (!core.getSettingsRegistry().get(Setting.SKIP_LOGIN_PROMPT)) {
            if (!isLoggedIn()) {
                new GuiLoginPrompt(apiClient, null, null, false).display();
            }
        }
    }

    public ReplayMod getCore() {
        return core;
    }

    public ReplayModReplay getReplayModule() {
        return replayModule;
    }

    public Logger getLogger() {
        return logger;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public boolean isLoggedIn() {
        return apiClient.isLoggedIn();
    }

    public File getDownloadsFolder() {
        return downloadsFolder;
    }

    public File getDownloadedFile(int id) {
        return new File(downloadsFolder, id + ".mcpr");
    }

    public boolean hasDownloaded(int id) {
        return getDownloadedFile(id).exists();
    }

    public void startReplay(int id, String name, GuiScreen onDownloadCancelled) throws IOException {
        File file = getDownloadedFile(id);
        if (file.exists()) {
            replayModule.startReplay(file);
        } else {
            new GuiReplayDownloading(onDownloadCancelled, this, id, name).display();
        }
    }
}
