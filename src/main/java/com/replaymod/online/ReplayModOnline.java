package com.replaymod.online;

import com.replaymod.core.ReplayMod;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.gui.GuiLoginPrompt;
import com.replaymod.online.gui.GuiReplayDownloading;
import com.replaymod.online.gui.GuiSaveModifiedReplay;
import com.replaymod.online.handler.GuiHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayCloseEvent;
import de.johni0702.minecraft.gui.container.GuiScreen;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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

    /**
     * In case the currently opened replay gets modified, the resulting replay file is saved to this location.
     * Usually a file within the normal replays folder with a unique name.
     * When the replay is closed, the user is asked whether they want to give it a proper name.
     */
    private File currentReplayOutputFile;

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
        FMLCommonHandler.instance().bus().register(this);
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
            currentReplayOutputFile = new File(core.getReplayFolder(), System.currentTimeMillis() + ".mcpr");
            ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), file, currentReplayOutputFile);
            replayModule.startReplay(replayFile);
        } else {
            new GuiReplayDownloading(onDownloadCancelled, this, id, name).display();
        }
    }

    @SubscribeEvent
    public void onReplayClosed(ReplayCloseEvent.Post event) {
        if (currentReplayOutputFile != null) {
            if (currentReplayOutputFile.exists()) { // Replay was modified, ask user for new name
                new GuiSaveModifiedReplay(currentReplayOutputFile).display();
            }
            currentReplayOutputFile = null;
        }
    }
}
