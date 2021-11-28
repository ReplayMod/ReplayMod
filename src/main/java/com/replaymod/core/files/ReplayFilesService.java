package com.replaymod.core.files;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.gui.RestoreReplayGui;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ReplayFilesService {
    private final ReplayFoldersService folders;

    public ReplayFilesService(ReplayFoldersService folders) {
        this.folders = folders;
    }

    public ReplayFile open(Path path) throws IOException {
        return open(path, path);
    }

    public ReplayFile open(Path input, Path output) throws IOException {
        return new ZipReplayFile(
                new ReplayStudio(),
                input != null ? input.toFile() : null,
                output.toFile(),
                folders.getCachePathForReplay(output).toFile()
        );
    }

    public void initialScan(ReplayMod core) {
        // Move anything which is still in the recording folder into the regular replay folder
        // so it can be opened and/or recovered
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getRecordingFolder())) {
            for (Path path : paths) {
                Path destination = folders.getReplayFolder().resolve(path.getFileName());
                if (Files.exists(destination)) {
                    continue; // better play it save
                }
                Files.move(path, destination);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Restore corrupted replays
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getReplayFolder())) {
            for (Path path : paths) {
                String name = path.getFileName().toString();
                if (name.endsWith(".mcpr.tmp") && Files.isDirectory(path)) {
                    Path original = path.resolveSibling(FilenameUtils.getBaseName(name));
                    Path noRecoverMarker = original.resolveSibling(original.getFileName() + ".no_recover");
                    if (Files.exists(noRecoverMarker)) {
                        // This file, when its markers are processed, doesn't actually result in any replays.
                        // So we don't really need to recover it either, let's just get rid of it.
                        FileUtils.deleteDirectory(path.toFile());
                        Files.delete(noRecoverMarker);
                        continue;
                    }
                    new RestoreReplayGui(core, GuiScreen.wrap(core.getMinecraft().currentScreen), original.toFile()).display();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Run general purpose, non-essential cleanup in a background thread
        new Thread(this::cleanup, "replaymod-cleanup").start();
    }

    private void cleanup() {
        final long DAYS = 24 * 60 * 60 * 1000;

        // Cleanup any cache folders still remaining in the recording folder (we once used to put them there)
        try {
            Files.walkFileTree(folders.getReplayFolder(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String name = dir.getFileName().toString();
                    if (name.endsWith(".mcpr.cache")) {
                        FileUtils.deleteDirectory(dir.toFile());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cleanup raw folder content three weeks after creation (these are pretty valuable for debugging)
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getRawReplayFolder())) {
            for (Path path : paths) {
                if (Files.getLastModifiedTime(path).toMillis() + 21 * DAYS < System.currentTimeMillis()) {
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cleanup cache folders 7 days after last modification or when its replay is gone
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getCacheFolder())) {
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    Path replay = folders.getReplayPathForCache(path);
                    long lastModified = Files.getLastModifiedTime(path).toMillis();
                    if (lastModified + 7 * DAYS < System.currentTimeMillis() || !Files.exists(replay)) {
                        FileUtils.deleteDirectory(path.toFile());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cleanup deleted corrupted replays
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getReplayFolder())) {
            for (Path path : paths) {
                String name = path.getFileName().toString();
                if (name.endsWith(".mcpr.del") && Files.isDirectory(path)) {
                    long lastModified = Files.getLastModifiedTime(path).toMillis();
                    if (lastModified + 2 * DAYS < System.currentTimeMillis()) {
                        FileUtils.deleteDirectory(path.toFile());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cleanup leftover no_recover files
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(folders.getReplayFolder())) {
            for (Path path : paths) {
                String name = path.getFileName().toString();
                if (name.endsWith(".no_recover")) {
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
