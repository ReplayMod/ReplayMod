package com.replaymod.recording.handler;

import com.replaymod.core.ReplayMod;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiRecordingOverlay;
import com.replaymod.recording.packet.PacketListener;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.studio.ReplayStudio;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Handles connection events and initiates recording if enabled.
 */
public class ConnectionEventHandler {

    private static final String packetHandlerKey = "packet_handler";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Logger logger;
    private final ReplayMod core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;
    }

    @SubscribeEvent
    public void onConnectedToServerEvent(ClientConnectedToServerEvent event) {
        try {
            if(event.isLocal) {
                if (MinecraftServer.getServer().getEntityWorld().getWorldType() == WorldType.DEBUG_WORLD) {
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                if(!core.getSettingsRegistry().get(Setting.RECORD_SINGLEPLAYER)) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if(!core.getSettingsRegistry().get(Setting.RECORD_SERVER)) {
                    logger.info("Multiplayer Recording is disabled");
                    return;
                }
            }

            NetworkManager nm = event.manager;
            String worldName;
            if(event.isLocal) {
                worldName = MinecraftServer.getServer().getWorldName();
            } else {
                worldName = Minecraft.getMinecraft().getCurrentServerData().serverIP;
            }
            Channel channel = nm.channel();
            ChannelPipeline pipeline = channel.pipeline();

            File folder = core.getReplayFolder();

            String name = sdf.format(Calendar.getInstance().getTime());
            File currentFile = new File(folder, name + ".mcpr");
            ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), currentFile);

            packetListener = new PacketListener(replayFile, name, worldName, System.currentTimeMillis(), event.isLocal);
            pipeline.addBefore(packetHandlerKey, "replay_recorder", packetListener);

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry());
            guiOverlay.register();

            core.printInfoToChat("replaymod.chat.recordingstarted");
        } catch(Exception e) {
            core.printWarningToChat("replaymod.chat.recordingfailed");
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
        if (packetListener != null) {
            guiOverlay.unregister();
            guiOverlay = null;
            recordingEventHandler.unregister();
            recordingEventHandler = null;
            packetListener = null;
        }
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }
}
