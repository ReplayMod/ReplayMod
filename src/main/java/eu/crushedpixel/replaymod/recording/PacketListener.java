package eu.crushedpixel.replaymod.recording;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Marker;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.util.HttpUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PacketListener extends DataListener {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private ChannelHandlerContext context = null;

    private int nextRequestId;

    public PacketListener(File file, String name, String worldName, long startTime, boolean singleplayer) throws FileNotFoundException {
        super(file, name, worldName, startTime, singleplayer);
    }

    public void saveOnly(Packet packet) {
        try {
            if(packet instanceof S0CPacketSpawnPlayer) {
                UUID uuid = ((S0CPacketSpawnPlayer) packet).func_179819_c();
                players.add(uuid.toString());
            }

            dataWriter.writePacket(getPacketData(packet));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(ctx == null) {
            if(context == null) {
                return;
            } else {
                ctx = context;
            }
        }
        this.context = ctx;
        if(!alive) {
            super.channelRead(ctx, msg);
            return;
        }
        if(msg instanceof Packet) {
            try {
                Packet packet = (Packet) msg;

                if(packet instanceof S0DPacketCollectItem) {
                    if(mc.thePlayer != null ||
                            ((S0DPacketCollectItem) packet).func_149353_d() == mc.thePlayer.getEntityId()) {
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                if(packet instanceof S0CPacketSpawnPlayer) {
                    UUID uuid = ((S0CPacketSpawnPlayer) packet).func_179819_c();
                    players.add(uuid.toString());
                }

                if (packet instanceof S48PacketResourcePackSend) {
                    S48PacketResourcePackSend p = (S48PacketResourcePackSend) packet;
                    int requestId = handleResourcePack(p);
                    saveOnly(new S48PacketResourcePackSend("replay://" + requestId, ""));
                    return;
                }

                dataWriter.writePacket(getPacketData(packet));
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

        super.channelRead(ctx, msg);
    }

    @SuppressWarnings("unchecked")
    private byte[] getPacketData(Packet packet) throws IOException {
        if(packet instanceof S0FPacketSpawnMob) {
            S0FPacketSpawnMob p = (S0FPacketSpawnMob) packet;
            if (p.field_149043_l == null) {
                p.field_149043_l = new DataWatcher(null);
                if(p.func_149027_c() != null) {
                    for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_149027_c()) {
                        p.field_149043_l.addObject(wo.getDataValueId(), wo.getObject());
                    }
                }
            }
        }

        if(packet instanceof S0CPacketSpawnPlayer) {
            S0CPacketSpawnPlayer p = (S0CPacketSpawnPlayer) packet;
            if (p.field_148960_i == null) {
                p.field_148960_i = new DataWatcher(null);
                if(p.func_148944_c() != null) {
                    for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_148944_c()) {
                        p.field_148960_i.addObject(wo.getDataValueId(), wo.getObject());
                    }
                }
            }
        }

        return ReplayFileIO.serializePacket(packet);
    }

    public synchronized int handleResourcePack(S48PacketResourcePackSend packet) {
        final int requestId = nextRequestId++;
        final NetHandlerPlayClient netHandler = mc.getNetHandler();
        final NetworkManager netManager = netHandler.getNetworkManager();
        final String url = packet.func_179783_a();
        final String hash = packet.func_179784_b();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.mcDataDir, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                Futures.addCallback(mc.getResourcePackRepository().func_177319_a(levelDir), new FutureCallback() {
                    public void onSuccess(Object result) {
                        recordResourcePack(levelDir, requestId);
                        netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                    }

                    public void onFailure(Throwable throwable) {
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
                mc.addScheduledTask(new Runnable() {
                    public void run() {
                        mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                            public void confirmClicked(boolean result, int id) {
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
                            }
                        }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0));
                    }
                });
            }
        }
        return requestId;
    }

    public void addMarker() {
        AdvancedPosition pos = new AdvancedPosition(Minecraft.getMinecraft().getRenderViewEntity(), false);
        int timestamp = (int) (System.currentTimeMillis() - startTime);

        Keyframe<Marker> marker = new Keyframe<Marker>(timestamp, new Marker(null, pos));

        markers.add(marker);

        ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.addedmarker", ChatMessageHandler.ChatMessageType.INFORMATION);
    }

    private void downloadResourcePackFuture(int requestId, String url, final String hash) {
        Futures.addCallback(downloadResourcePack(requestId, url, hash), new FutureCallback() {
            public void onSuccess(Object result) {
                mc.getNetHandler().addToSendQueue(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
            }

            public void onFailure(Throwable throwable) {
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
        repo.field_177321_h.lock();
        try {
            repo.func_148529_f();

            if (file.exists() && hash.length() == 40) {
                try {
                    String fileHash = Hashing.sha1().hashBytes(Files.toByteArray(file)).toString();
                    if (fileHash.equals(hash)) {
                        recordResourcePack(file, requestId);
                        return repo.func_177319_a(file);
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

            Futures.getUnchecked(mc.addScheduledTask(new Runnable() {
                public void run() {
                    mc.displayGuiScreen(guiScreen);
                }
            }));

            Map sessionInfo = Minecraft.getSessionInfo();
            repo.field_177322_i = HttpUtil.func_180192_a(file, url, sessionInfo, 50 * 1024 * 1024, guiScreen, mc.getProxy());
            Futures.addCallback(repo.field_177322_i, new FutureCallback() {
                public void onSuccess(Object value) {
                    recordResourcePack(file, requestId);
                    repo.func_177319_a(file);
                }

                public void onFailure(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            return repo.field_177322_i;
        } finally {
            repo.field_177321_h.unlock();
        }
    }

}
