package com.replaymod.online;

import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.AuthData;
import com.replaymod.online.gui.GuiLoginPrompt;
import com.replaymod.online.gui.GuiReplayDownloading;
import com.replaymod.online.gui.GuiSaveModifiedReplay;
import com.replaymod.online.handler.GuiHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.GuiScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.common.config.Configuration;
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif
//#endif

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.replaymod.core.versions.MCVer.*;

public class ReplayModOnline implements Module {
    { instance = this; }
    public static ReplayModOnline instance;

    private ReplayMod core;

    private ReplayModReplay replayModule;

    public static Logger LOGGER = LogManager.getLogger();

    private ApiClient apiClient;

    /**
     * In case the currently opened replay gets modified, the resulting replay file is saved to this location.
     * Usually a file within the normal replays folder with a unique name.
     * When the replay is closed, the user is asked whether they want to give it a proper name.
     */
    private File currentReplayOutputFile;

    public ReplayModOnline(ReplayMod core, ReplayModReplay replayModule) {
        this.core = core;
        this.replayModule = replayModule;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void initClient() {
        Path path = MCVer.mcDataDir(MCVer.getMinecraft()).toPath().resolve("config/replaymod-online.token");
        PlainFileAuthData authData = new PlainFileAuthData(path);
        apiClient = new ApiClient(authData);
        if (Files.notExists(path)) {
            AuthData oldAuthData = loadOldAuthData();
            if (oldAuthData != null) {
                authData.setData(oldAuthData.getUserName(), oldAuthData.getAuthKey());
            }
        } else {
            try {
                authData.load(apiClient);
            } catch (IOException e) {
                ReplayModOnline.LOGGER.error("Loading auth data:", e);
            }
        }

        if (!getDownloadsFolder().exists()){
            if (!getDownloadsFolder().mkdirs()) {
                LOGGER.warn("Failed to create downloads folder: " + getDownloadsFolder());
            }
        }

        new GuiHandler(this).register();
        FML_BUS.register(this);

        // Initial login prompt
        if (!core.getSettingsRegistry().get(Setting.SKIP_LOGIN_PROMPT)) {
            if (!isLoggedIn()) {
                core.runLater(() -> {
                    GuiScreen parent = GuiScreen.wrap(getMinecraft().currentScreen);
                    new GuiLoginPrompt(apiClient, parent, parent, false).display();
                });
            }
        }
    }

    /**
     * Loads old auth data from the legacy Configuration system and stores it in the new json file.
     * Always returns null on 1.13+ where the Configuration system has been removed.
     */
    private AuthData loadOldAuthData() {
        //#if MC<11300
        //$$ Path path = MCVer.mcDataDir(MCVer.getMinecraft()).toPath().resolve("config/replaymod-online.cfg");
        //$$ Configuration config = new Configuration(path.toFile());
        //$$ ConfigurationAuthData authData = new ConfigurationAuthData(config);
        //$$ authData.load(new ApiClient(authData));
        //$$ if (authData.getAuthKey() != null) {
        //$$     return authData;
        //$$ }
        //#endif
        return null;
    }

    public ReplayMod getCore() {
        return core;
    }

    public ReplayModReplay getReplayModule() {
        return replayModule;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public boolean isLoggedIn() {
        return apiClient.isLoggedIn();
    }

    public File getDownloadsFolder() {
        String path = core.getSettingsRegistry().get(Setting.DOWNLOAD_PATH);
        return new File(path.startsWith("./") ? mcDataDir(getMinecraft()) : null, path);
    }

    public File getDownloadedFile(int id) {
        return new File(getDownloadsFolder(), id + ".mcpr");
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
