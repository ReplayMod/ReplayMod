package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.ServerResourceMode;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.HttpUtil;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public synchronized S3FPacketCustomPayload handleResourcePack(S3FPacketCustomPayload packet) {
        final int requestId = nextRequestId++;
        final String url = new String(packet.func_149168_d(), Charsets.UTF_8);

        ServerData serverData = mc.getCurrentServerData();
        ServerResourceMode resourceMode = serverData == null ? ServerResourceMode.PROMPT : serverData.getResourceMode();
        if (resourceMode == ServerResourceMode.ENABLED) {
            downloadResourcePack(requestId, url);
            mc.getResourcePackRepository().obtainResourcePack(url);
        } else if (resourceMode == ServerResourceMode.PROMPT) {
            // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
            //noinspection Convert2Lambda
            mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                @Override
                public void confirmClicked(boolean result, int id) {
                    if (serverData != null) {
                        serverData.setResourceMode(ServerResourceMode.ENABLED);
                        ServerList.func_147414_b(serverData);
                    }

                    mc.displayGuiScreen(null);

                    if (result) {
                        downloadResourcePack(requestId, url);
                    }
                }
            }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0));
        }

        return new S3FPacketCustomPayload(packet.func_149169_c(), ("replay://" + requestId).getBytes(Charsets.UTF_8));
    }

    private void downloadResourcePack(final int requestId, String url) {
        final ResourcePackRepository repo = mc.mcResourcePackRepository;

        String fileName = url.substring(url.lastIndexOf("/") + 1);

        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        if (!fileName.endsWith(".zip")) {
            return;
        }

        fileName = fileName.replaceAll("\\W", "");

        File file = new File(repo.field_148534_e, fileName);

        HashMap<String, String> hashmap = new HashMap<>();
        hashmap.put("X-Minecraft-Username", mc.getSession().getUsername());
        hashmap.put("X-Minecraft-UUID", mc.getSession().getPlayerID());
        hashmap.put("X-Minecraft-Version", "1.7.10");

        GuiScreenWorking guiScreen = new GuiScreenWorking();
        Minecraft.getMinecraft().displayGuiScreen(guiScreen);
        repo.func_148529_f();
        repo.field_148533_g = true;
        // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
        //noinspection Convert2Lambda
        HttpUtil.downloadResourcePack(file, url, new HttpUtil.DownloadListener() {
            public void onDownloadComplete(File file) {
                if (repo.field_148533_g) {
                    repo.field_148533_g = false;
                    repo.field_148532_f = new FileResourcePack(file);
                    Minecraft.getMinecraft().scheduleResourcesRefresh();
                    recordResourcePack(file, requestId);
                }
            }
        }, hashmap, 50*1024*1024, guiScreen, Minecraft.getMinecraft().getProxy());
    }
}
