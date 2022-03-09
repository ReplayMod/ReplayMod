package com.replaymod.core.files;

import com.google.common.net.PercentEscaper;
import com.replaymod.core.Setting;
import com.replaymod.core.SettingsRegistry;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.replaymod.core.utils.Utils.ensureDirectoryExists;

public class ReplayFoldersService {
    private final Path mcDir = MinecraftClient.getInstance().runDirectory.toPath();
    private final SettingsRegistry settings;

    public ReplayFoldersService(SettingsRegistry settings) {
        this.settings = settings;
    }

    public Path getReplayFolder() throws IOException {
        return ensureDirectoryExists(mcDir.resolve(settings.get(Setting.RECORDING_PATH)));
    }

    /**
     * Folder into which replay backups are saved before the MarkerProcessor is unleashed.
     */
    public Path getRawReplayFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("raw"));
    }

    /**
     * Folder into which replays are recorded.
     * Distinct from the main folder, so they cannot be opened while they are still saving.
     */
    public Path getRecordingFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("recording"));
    }

    /**
     * Folder in which replay cache files are stored.
     * Distinct from the recording folder cause people kept confusing them with recordings.
     */
    public Path getCacheFolder() throws IOException {
        Path path = ensureDirectoryExists(mcDir.resolve(settings.get(Setting.CACHE_PATH)));
        try {
            Files.setAttribute(path, "dos:hidden", true);
        } catch (UnsupportedOperationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    private static final PercentEscaper CACHE_FILE_NAME_ENCODER = new PercentEscaper("-_ ", false);

    public Path getCachePathForReplay(Path replay) throws IOException {
        Path replayFolder = getReplayFolder();
        Path cacheFolder = getCacheFolder();
        Path relative = replayFolder.toAbsolutePath().relativize(replay.toAbsolutePath());
        return cacheFolder.resolve(CACHE_FILE_NAME_ENCODER.escape(relative.toString()));
    }

    public Path getReplayPathForCache(Path cache) throws IOException {
        String relative = URLDecoder.decode(cache.getFileName().toString(), "UTF-8");
        Path replayFolder = getReplayFolder();
        return replayFolder.resolve(relative);
    }
}
