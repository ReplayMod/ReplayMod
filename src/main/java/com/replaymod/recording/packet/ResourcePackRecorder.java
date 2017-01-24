package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.util.HttpUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Records resource packs and handles incoming resource pack packets during recording.
 */
public class ResourcePackRecorder {
    private static final Logger logger = LogManager.getLogger();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ReplayFile replayFile;

    private int nextRequestId;

    public ResourcePackRecorder(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    public void recordResourcePack(File file, int requestId) {
        try {
            // Read in resource pack file
            byte[] bytes = Files.toByteArray(file);
            // Check whether it is already known
            String hash = Hashing.sha1().hashBytes(bytes).toString();
            boolean doWrite = false; // Whether we are the first and have to write it
            synchronized (replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index == null) {
                    index = new HashMap<>();
                }
                if (!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if (doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    public synchronized S48PacketResourcePackSend handleResourcePack(S48PacketResourcePackSend packet) {
        final int requestId = nextRequestId++;
        final NetHandlerPlayClient netHandler = mc.getNetHandler();
        final NetworkManager netManager = netHandler.getNetworkManager();
        final String url = packet.getURL();
        final String hash = packet.getHash();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.mcDataDir, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                Futures.addCallback(mc.getResourcePackRepository().setResourcePackInstance(levelDir), new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(Object result) {
                        recordResourcePack(levelDir, requestId);
                        netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                    }

                    @Override
                    public void onFailure(@Nonnull Throwable throwable) {
                        netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                    }
                });
            } else {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
            }
        } else {
            final ServerData serverData = mc.getCurrentServerData();
            if (serverData != null && serverData.getResourceMode() == ServerData.ServerResourceMode.ENABLED) {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                downloadResourcePackFuture(requestId, url, hash);
            } else if (serverData != null && serverData.getResourceMode() != ServerData.ServerResourceMode.PROMPT) {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.DECLINED));
            } else {
                mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiYesNo((result, id) -> {
                    if (serverData != null) {
                        serverData.setResourceMode(result ? ServerData.ServerResourceMode.ENABLED : ServerData.ServerResourceMode.DISABLED);
                    }
                    if (result) {
                        netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                        downloadResourcePackFuture(requestId, url, hash);
                    } else {
                        netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.DECLINED));
                    }

                    ServerList.func_147414_b(serverData);
                    mc.displayGuiScreen(null);
                }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0)));
            }
        }

        return new S48PacketResourcePackSend("replay://" + requestId, "");
    }

    private void downloadResourcePackFuture(int requestId, String url, final String hash) {
        Futures.addCallback(downloadResourcePack(requestId, url, hash), new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                mc.getNetHandler().addToSendQueue(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
                mc.getNetHandler().addToSendQueue(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
            }
        });
    }

    private ListenableFuture downloadResourcePack(final int requestId, String url, String hash) {
        final ResourcePackRepository repo = mc.mcResourcePackRepository;
        String fileName;
        if (hash.matches("^[a-f0-9]{40}$")) {
            fileName = hash;
        } else {
            fileName = url.substring(url.lastIndexOf("/") + 1);

            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }

            if (!fileName.endsWith(".zip")) {
                return Futures.immediateFailedFuture(new IllegalArgumentException("Invalid filename; must end in .zip"));
            }

            fileName = "legacy_" + fileName.replaceAll("\\W", "");
        }

        final File file = new File(repo.dirServerResourcepacks, fileName);
        repo.lock.lock();
        try {
            repo.clearResourcePack();

            if (file.exists() && hash.length() == 40) {
                try {
                    String fileHash = Hashing.sha1().hashBytes(Files.toByteArray(file)).toString();
                    if (fileHash.equals(hash)) {
                        recordResourcePack(file, requestId);
                        return repo.setResourcePackInstance(file);
                    }

                    logger.warn("File " + file + " had wrong hash (expected " + hash + ", found " + fileHash + "). Deleting it.");
                    FileUtils.deleteQuietly(file);
                } catch (IOException ioexception) {
                    logger.warn("File " + file + " couldn\'t be hashed. Deleting it.", ioexception);
                    FileUtils.deleteQuietly(file);
                }
            }

            final GuiScreenWorking guiScreen = new GuiScreenWorking();
            final Minecraft mc = Minecraft.getMinecraft();

            Futures.getUnchecked(mc.addScheduledTask(() -> mc.displayGuiScreen(guiScreen)));

            Map<String, String> sessionInfo = Minecraft.getSessionInfo();
            repo.downloadingPacks = HttpUtil.downloadResourcePack(file, url, sessionInfo, 50 * 1024 * 1024, guiScreen, mc.getProxy());
            Futures.addCallback(repo.downloadingPacks, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object value) {
                    recordResourcePack(file, requestId);
                    repo.setResourcePackInstance(file);
                }

                @Override
                public void onFailure(@Nonnull Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            return repo.downloadingPacks;
        } finally {
            repo.lock.unlock();
        }
    }

}
