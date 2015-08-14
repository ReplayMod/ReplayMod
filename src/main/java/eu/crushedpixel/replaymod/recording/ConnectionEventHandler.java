package eu.crushedpixel.replaymod.recording;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.gui.overlay.GuiRecordingOverlay;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ConnectionEventHandler {

    private static final String packetHandlerKey = "packet_handler";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static PacketListener packetListener = null;
    private static boolean isRecording = false;
    private final GuiRecordingOverlay guiOverlay = new GuiRecordingOverlay(Minecraft.getMinecraft());

    private static final Logger logger = LogManager.getLogger();

    public static boolean isRecording() {
        return isRecording;
    }

    public static void notifyServerPaused() {
        if (packetListener != null) {
            packetListener.serverWasPaused = true;
        }
    }

    public static void insertPacket(Packet packet) {
        if(!isRecording || packetListener == null) {
            String reason = isRecording ? " (recording)" : " (null)";
            logger.error("Invalid attempt to insert Packet!" + reason);
            return;
        }
        try {
            packetListener.saveOnly(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void addMarker() {
        if(!isRecording || packetListener == null) {
            String reason = isRecording ? " (recording)" : " (null)";
            logger.error("Invalid attempt to insert MarkerKeyframe!" + reason);
            return;
        }
        try {
            packetListener.addMarker();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onConnectedToServerEvent(ClientConnectedToServerEvent event) {
        ReplayMod.chatMessageHandler.initialize();
        ReplayMod.recordingHandler.resetVars();

        try {
            if(event.isLocal) {
                if (MinecraftServer.getServer().getEntityWorld().getWorldType() == WorldType.DEBUG_WORLD) {
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                if(!ReplayMod.replaySettings.isEnableRecordingSingleplayer()) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if(!ReplayMod.replaySettings.isEnableRecordingServer()) {
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

            File folder = ReplayFileIO.getReplayFolder();

            String fileName = sdf.format(Calendar.getInstance().getTime());
            File currentFile = new File(folder, fileName + ReplayFile.TEMP_FILE_EXTENSION);

            pipeline.addBefore(packetHandlerKey, "replay_recorder", packetListener = new PacketListener
                    (currentFile, fileName, worldName, System.currentTimeMillis(), event.isLocal));
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.recordingstarted", ChatMessageType.INFORMATION);
            isRecording = true;

            MinecraftForge.EVENT_BUS.register(guiOverlay);
        } catch(Exception e) {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.recordingfailed", ChatMessageType.WARNING);
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
        if (isRecording) {
            isRecording = false;
            MinecraftForge.EVENT_BUS.unregister(guiOverlay);
            packetListener = null;
            ReplayMod.chatMessageHandler.stop();
        }
    }
}
