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
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.HttpUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=10904
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.client.CPacketResourcePackStatus.Action;
import net.minecraft.network.play.server.SPacketResourcePackSend;
//#else
//$$ import net.minecraft.network.play.client.C19PacketResourcePackStatus;
//$$ import net.minecraft.network.play.client.C19PacketResourcePackStatus.Action;
//$$ import net.minecraft.network.play.server.S48PacketResourcePackSend;
//#endif

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

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

    //#if MC>=10904
    public CPacketResourcePackStatus makeStatusPacket(String hash, Action action) {
        //#if MC>=11002
        return new CPacketResourcePackStatus(action);
        //#else
        //$$ return new CPacketResourcePackStatus(hash, action);
        //#endif
    }
    //#else
    //$$ public C19PacketResourcePackStatus makeStatusPacket(String hash, Action action) {
    //$$     return new C19PacketResourcePackStatus(hash, action);
    //$$ }
    //#endif


    //#if MC>=10904
    public synchronized SPacketResourcePackSend handleResourcePack(SPacketResourcePackSend packet) {
    //#else
    //$$ public synchronized S48PacketResourcePackSend handleResourcePack(S48PacketResourcePackSend packet) {
    //#endif
        final int requestId = nextRequestId++;
        final NetHandlerPlayClient netHandler = getConnection(mc);
        final NetworkManager netManager = netHandler.getNetworkManager();
        //#if MC>=10809
        final String url = packet.getURL();
        final String hash = packet.getHash();
        //#else
        //$$ final String url = packet.func_179783_a();
        //$$ final String hash = packet.func_179784_b();
        //#endif

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.mcDataDir, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                Futures.addCallback(setServerResourcePack(mc.getResourcePackRepository(), levelDir), new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(Object result) {
                        recordResourcePack(levelDir, requestId);
                        netManager.sendPacket(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED));
                    }

                    @Override
                    public void onFailure(@Nonnull Throwable throwable) {
                        netManager.sendPacket(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
                    }
                });
            } else {
                netManager.sendPacket(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
            }
        } else {
            final ServerData serverData = mc.getCurrentServerData();
            if (serverData != null && serverData.getResourceMode() == ServerData.ServerResourceMode.ENABLED) {
                netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                downloadResourcePackFuture(requestId, url, hash);
            } else if (serverData != null && serverData.getResourceMode() != ServerData.ServerResourceMode.PROMPT) {
                netManager.sendPacket(makeStatusPacket(hash, Action.DECLINED));
            } else {
                // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
                //noinspection Convert2Lambda
                mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                    @Override
                    public void confirmClicked(boolean result, int id) {
                        if (serverData != null) {
                            serverData.setResourceMode(result ? ServerData.ServerResourceMode.ENABLED : ServerData.ServerResourceMode.DISABLED);
                        }
                        if (result) {
                            netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                            downloadResourcePackFuture(requestId, url, hash);
                        } else {
                            netManager.sendPacket(makeStatusPacket(hash, Action.DECLINED));
                        }

                        ServerList_saveSingleServer(serverData);
                        mc.displayGuiScreen(null);
                    }
                }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0)));
            }
        }

        //#if MC>=10904
        return new SPacketResourcePackSend("replay://" + requestId, "");
        //#else
        //$$ return new S48PacketResourcePackSend("replay://" + requestId, "");
        //#endif
    }

    private void downloadResourcePackFuture(int requestId, String url, final String hash) {
        Futures.addCallback(downloadResourcePack(requestId, url, hash), new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                sendPacket(getConnection(mc), makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED));
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
                sendPacket(getConnection(mc), makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
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
        //#if MC>=10809
        repo.lock.lock();
        //#else
        //$$ repo.field_177321_h.lock();
        //#endif
        try {
            //#if MC>=10809
            repo.clearResourcePack();
            //#else
            //$$ repo.func_148529_f();
            //#endif

            if (file.exists() && hash.length() == 40) {
                try {
                    String fileHash = Hashing.sha1().hashBytes(Files.toByteArray(file)).toString();
                    if (fileHash.equals(hash)) {
                        recordResourcePack(file, requestId);
                        return setServerResourcePack(repo, file);
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

            //#if MC>=10809
            //#if MC>=11002
            //#if MC>=11100
            Map<String, String> sessionInfo = ResourcePackRepository.getDownloadHeaders();
            //#else
            //$$ Map<String, String> sessionInfo = ResourcePackRepository.func_190115_a();
            //#endif
            //#else
            //$$ Map<String, String> sessionInfo = Minecraft.getSessionInfo();
            //#endif
            repo.downloadingPacks = HttpUtil.downloadResourcePack(file, url, sessionInfo, 50 * 1024 * 1024, guiScreen, mc.getProxy());
            Futures.addCallback(repo.downloadingPacks, new FutureCallback<Object>() {
            //#else
            //$$ Map sessionInfo = Minecraft.getSessionInfo();
            //$$ repo.field_177322_i = HttpUtil.func_180192_a(file, url, sessionInfo, 50 * 1024 * 1024, guiScreen, mc.getProxy());
            //$$ Futures.addCallback(repo.field_177322_i, new FutureCallback() {
            //#endif
                @Override
                public void onSuccess(Object value) {
                    recordResourcePack(file, requestId);
                    setServerResourcePack(repo, file);
                }

                @Override
                public void onFailure(@Nonnull Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            //#if MC>=10809
            return repo.downloadingPacks;
            //#else
            //$$ return repo.field_177322_i;
            //#endif
        } finally {
            //#if MC>=10809
            repo.lock.unlock();
            //#else
            //$$ repo.field_177321_h.unlock();
            //#endif
        }
    }

}
