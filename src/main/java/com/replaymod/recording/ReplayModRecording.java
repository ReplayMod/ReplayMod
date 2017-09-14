package com.replaymod.recording;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.recording.packet.PacketListener;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.network.NetworkRegistry;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@Mod(modid = ReplayModRecording.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        useMetadata = true)
public class ReplayModRecording {
    public static final String MOD_ID = "replaymod-recording";

    @Mod.Instance(MOD_ID)
    public static ReplayModRecording instance;

    private ReplayMod core;

    private Logger logger;

    private ConnectionEventHandler connectionEventHandler;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        core = ReplayMod.instance;

        core.getSettingsRegistry().register(Setting.class);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, new Runnable() {
            @Override
            public void run() {
                PacketListener packetListener = connectionEventHandler.getPacketListener();
                if (packetListener != null) {
                    packetListener.addMarker();
                    core.printInfoToChat("replaymod.chat.addedmarker");
                }
            }
        });
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EventBus bus = FMLCommonHandler.instance().bus();
        bus.register(connectionEventHandler = new ConnectionEventHandler(logger, core));

        NetworkRegistry.INSTANCE.newChannel(Restrictions.PLUGIN_CHANNEL, new RestrictionsChannelHandler());
    }

    @ChannelHandler.Sharable
    private static class RestrictionsChannelHandler extends ChannelDuplexHandler {}

    public void initiateRecording(NetworkManager networkManager) {
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }
}
