package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.options.ServerEntry;
import net.minecraft.client.options.ServerList;
import net.minecraft.client.resource.ClientResourcePackCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11400
import net.minecraft.text.TranslatableText;
//#else
//$$ import net.minecraft.client.gui.GuiYesNoCallback;
//$$ import net.minecraft.client.resources.I18n;
//#endif

//#if MC>=10800
import de.johni0702.minecraft.gui.utils.Consumer;
//#else
//$$ import net.minecraft.client.gui.GuiScreenWorking;
//$$ import net.minecraft.util.HttpUtil;
//#endif

//#if MC>=10800
import net.minecraft.server.network.packet.ResourcePackStatusC2SPacket;
import net.minecraft.server.network.packet.ResourcePackStatusC2SPacket.Status;
import net.minecraft.client.network.packet.ResourcePackSendS2CPacket;
//#endif

//#if MC>=10800
//#if MC>=11400
import java.util.concurrent.CompletableFuture;
//#else
//$$ import com.google.common.util.concurrent.ListenableFuture;
//#endif
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
//#else
//$$ import com.replaymod.core.mixin.ResourcePackRepositoryAccessor;
//$$ import net.minecraft.client.multiplayer.ServerData.ServerResourceMode;
//$$ import net.minecraft.client.multiplayer.ServerList;
//$$ import net.minecraft.client.resources.FileResourcePack;
//$$ import net.minecraft.network.play.server.S3FPacketCustomPayload;
//$$ import org.apache.commons.io.Charsets;
//#endif

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
    private static final MinecraftClient mc = getMinecraft();

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

    //#if MC>=10800
    public ResourcePackStatusC2SPacket makeStatusPacket(String hash, Status action) {
        //#if MC>=11002
        return new ResourcePackStatusC2SPacket(action);
        //#else
        //$$ return new CPacketResourcePackStatus(hash, action);
        //#endif
    }


    public synchronized ResourcePackSendS2CPacket handleResourcePack(ResourcePackSendS2CPacket packet) {
        final int requestId = nextRequestId++;
        final ClientPlayNetworkHandler netHandler = mc.getNetworkHandler();
        final ClientConnection netManager = netHandler.getConnection();
        final String url = packet.getURL();
        final String hash = packet.getSHA1();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.runDirectory, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.send(makeStatusPacket(hash, Status.ACCEPTED));
                addCallback(setServerResourcePack(levelDir), result -> {
                    recordResourcePack(levelDir, requestId);
                    netManager.send(makeStatusPacket(hash, Status.SUCCESSFULLY_LOADED));
                }, throwable -> {
                    netManager.send(makeStatusPacket(hash, Status.FAILED_DOWNLOAD));
                });
            } else {
                netManager.send(makeStatusPacket(hash, Status.FAILED_DOWNLOAD));
            }
        } else {
            final ServerEntry serverData = mc.getCurrentServerEntry();
            if (serverData != null && serverData.getResourcePack() == ServerEntry.ResourcePackState.ENABLED) {
                netManager.send(makeStatusPacket(hash, Status.ACCEPTED));
                downloadResourcePackFuture(requestId, url, hash);
            } else if (serverData != null && serverData.getResourcePack() != ServerEntry.ResourcePackState.PROMPT) {
                netManager.send(makeStatusPacket(hash, Status.DECLINED));
            } else {
                // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
                //#if MC>=11400
                mc.execute(() -> mc.openScreen(new ConfirmScreen(result -> {
                //#else
                //$$ //noinspection Convert2Lambda
                //$$ mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                //$$     @Override
                //$$     public void confirmClicked(boolean result, int id) {
                //#endif
                        if (serverData != null) {
                            serverData.setResourcePackState(result ? ServerEntry.ResourcePackState.ENABLED : ServerEntry.ResourcePackState.DISABLED);
                        }
                        if (result) {
                            netManager.send(makeStatusPacket(hash, Status.ACCEPTED));
                            downloadResourcePackFuture(requestId, url, hash);
                        } else {
                            netManager.send(makeStatusPacket(hash, Status.DECLINED));
                        }

                        ServerList.updateServerListEntry(serverData);
                        mc.openScreen(null);
                    }
                //#if MC>=11400
                , new TranslatableText("multiplayer.texturePrompt.line1"), new TranslatableText("multiplayer.texturePrompt.line2"))));
                //#else
                //$$ }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0)));
                //#endif
            }
        }

        return new ResourcePackSendS2CPacket("replay://" + requestId, "");
    }

    private void downloadResourcePackFuture(int requestId, String url, final String hash) {
        addCallback(downloadResourcePack(requestId, url, hash),
                result -> mc.getNetworkHandler().sendPacket(makeStatusPacket(hash, Status.SUCCESSFULLY_LOADED)),
                throwable -> mc.getNetworkHandler().sendPacket(makeStatusPacket(hash, Status.FAILED_DOWNLOAD)));
    }

    private
    //#if MC>=11400
    CompletableFuture<?>
    //#else
    //$$ ListenableFuture<?>
    //#endif
    downloadResourcePack(final int requestId, String url, String hash) {
        ClientResourcePackCreator packFinder = mc.getResourcePackDownloader();
        ((IDownloadingPackFinder) packFinder).setRequestCallback(file -> recordResourcePack(file, requestId));
        return packFinder.download(url, hash);
    }

    public interface IDownloadingPackFinder {
        void setRequestCallback(Consumer<File> callback);
    }
    //#else
    //$$ public synchronized S3FPacketCustomPayload handleResourcePack(S3FPacketCustomPayload packet) {
    //$$     final int requestId = nextRequestId++;
    //$$     final String url = new String(packet.func_149168_d(), Charsets.UTF_8);
    //$$
    //$$     ServerData serverData = mc.getCurrentServerData();
    //$$     ServerResourceMode resourceMode = serverData == null ? ServerResourceMode.PROMPT : serverData.getResourceMode();
    //$$     if (resourceMode == ServerResourceMode.ENABLED) {
    //$$         downloadResourcePack(requestId, url);
    //$$         mc.getResourcePackRepository().obtainResourcePack(url);
    //$$     } else if (resourceMode == ServerResourceMode.PROMPT) {
    //$$         // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
    //$$         //noinspection Convert2Lambda
    //$$         mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
    //$$             @Override
    //$$             public void confirmClicked(boolean result, int id) {
    //$$                 if (serverData != null) {
    //$$                     serverData.setResourceMode(ServerResourceMode.ENABLED);
    //$$                     ServerList.func_147414_b(serverData);
    //$$                 }
    //$$
    //$$                 mc.displayGuiScreen(null);
    //$$
    //$$                 if (result) {
    //$$                     downloadResourcePack(requestId, url);
    //$$                 }
    //$$             }
    //$$         }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0));
    //$$     }
    //$$
    //$$     return new S3FPacketCustomPayload(packet.func_149169_c(), ("replay://" + requestId).getBytes(Charsets.UTF_8));
    //$$ }
    //$$
    //$$ private void downloadResourcePack(final int requestId, String url) {
    //$$     final ResourcePackRepository repo = mc.getResourcePackRepository();
    //$$     final ResourcePackRepositoryAccessor acc = (ResourcePackRepositoryAccessor) repo;
    //$$
    //$$     String fileName = url.substring(url.lastIndexOf("/") + 1);
    //$$
    //$$     if (fileName.contains("?")) {
    //$$         fileName = fileName.substring(0, fileName.indexOf("?"));
    //$$     }
    //$$
    //$$     if (!fileName.endsWith(".zip")) {
    //$$         return;
    //$$     }
    //$$
    //$$     fileName = fileName.replaceAll("\\W", "");
    //$$
    //$$     File file = new File(acc.getCacheDir(), fileName);
    //$$
    //$$     HashMap<String, String> hashmap = new HashMap<>();
    //$$     hashmap.put("X-Minecraft-Username", mc.getSession().getUsername());
    //$$     hashmap.put("X-Minecraft-UUID", mc.getSession().getPlayerID());
    //$$     hashmap.put("X-Minecraft-Version", "1.7.10");
    //$$
    //$$     GuiScreenWorking guiScreen = new GuiScreenWorking();
    //$$     Minecraft.getMinecraft().displayGuiScreen(guiScreen);
    //$$     repo.func_148529_f();
    //$$     acc.setActive(true);
    //$$     // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
    //$$     //noinspection Convert2Lambda
    //$$     HttpUtil.downloadResourcePack(file, url, new HttpUtil.DownloadListener() {
    //$$         public void onDownloadComplete(File file) {
    //$$             if (acc.isActive()) {
    //$$                 acc.setActive(false);
    //$$                 acc.setPack(new FileResourcePack(file));
    //$$                 Minecraft.getMinecraft().scheduleResourcesRefresh();
    //$$                 recordResourcePack(file, requestId);
    //$$             }
    //$$         }
    //$$     }, hashmap, 50*1024*1024, guiScreen, Minecraft.getMinecraft().getProxy());
    //$$ }
    //#endif

}
