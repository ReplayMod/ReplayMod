package com.replaymod.recording.handler;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.core.utils.Utils;
import com.replaymod.editor.gui.MarkerProcessor;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiRecordingControls;
import com.replaymod.recording.gui.GuiRecordingOverlay;
import com.replaymod.recording.mixin.NetworkManagerAccessor;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import io.netty.channel.Channel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import org.apache.logging.log4j.Logger;

//#if MC>=11600
import net.minecraft.world.World;
//#else
//#if MC>=11400
//$$ import net.minecraft.world.dimension.DimensionType;
//#endif
//$$
//#if MC>=10800
//$$ import net.minecraft.world.level.LevelGeneratorType;
//#endif
//#endif

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.replaymod.core.versions.MCVer.getMinecraft;

/**
 * Handles connection events and initiates recording if enabled.
 */
public class ConnectionEventHandler {

    private static final String packetHandlerKey = "packet_handler";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final MinecraftClient mc = getMinecraft();

    private final Logger logger;
    private final ReplayMod core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;
    private GuiRecordingControls guiControls;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;
    }

    public void onConnectedToServerEvent(ClientConnection networkManager) {
        try {
            boolean local = networkManager.isLocal();
            if (local) {
                //#if MC>=10800
                //#if MC>=11600
                if (mc.getServer().getWorld(World.OVERWORLD).isDebugWorld()) {
                //#else
                //#if MC>=11400
                //$$ if (mc.getServer().getWorld(DimensionType.OVERWORLD).getGeneratorType() == LevelGeneratorType.DEBUG_ALL_BLOCK_STATES) {
                //#else
                //$$ if (mc.getIntegratedServer().getEntityWorld().getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
                //#endif
                //#endif
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                //#endif
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

            String worldName;
            if (local) {
                //#if MC>=11600
                worldName = mc.getServer().getSaveProperties().getLevelName();
                //#else
                //$$ worldName = mc.getServer().getLevelName();
                //#endif
            } else if (mc.getCurrentServerEntry() != null) {
                worldName = mc.getCurrentServerEntry().address;
            //#if MC>=11100
            } else if (mc.isConnectedToRealms()) {
                // we can't access the server name without tapping too deep in the Realms Library
                worldName = "A Realms Server";
            //#endif
            } else {
                logger.info("Recording not started as the world is neither local nor remote (probably a replay).");
                return;
            }

            File folder = core.getReplayFolder();

            String name = sdf.format(Calendar.getInstance().getTime());
            File currentFile = new File(folder, Utils.replayNameToFileName(name));
            ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), currentFile);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setGenerator("ReplayMod v" + ReplayMod.instance.getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(ReplayMod.getMinecraftVersion());
            packetListener = new PacketListener(core, currentFile.toPath(), replayFile, metaData);
            Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
            channel.pipeline().addBefore(packetHandlerKey, "replay_recorder", packetListener);

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiControls = new GuiRecordingControls(core, packetListener);
            guiControls.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry(), guiControls);
            guiOverlay.register();

            if (core.getSettingsRegistry().get(Setting.AUTO_START_RECORDING) || ReplayMod.isMinimalMode()) {
                core.printInfoToChat("replaymod.chat.recordingstarted");
            } else {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, 0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("replaymod.chat.recordingfailed");
        }
    }

    public void reset() {
        if (packetListener != null) {
            guiControls.unregister();
            guiControls = null;
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
