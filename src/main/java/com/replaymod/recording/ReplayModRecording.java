package com.replaymod.recording;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.recording.handler.GuiHandler;
import com.replaymod.recording.packet.PacketListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11300
import com.replaymod.core.versions.MCVer.Keyboard;
//#else
//$$ import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.fml.network.NetworkRegistry;
//#else
//$$ import net.minecraftforge.fml.common.network.NetworkRegistry;
//#endif
//#else
//$$ import cpw.mods.fml.common.network.NetworkRegistry;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class ReplayModRecording implements Module {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AttributeKey<Void> ATTR_CHECKED = AttributeKey.newInstance("ReplayModRecording_checked");

    { instance = this; }
    public static ReplayModRecording instance;

    private ReplayMod core;

    private ConnectionEventHandler connectionEventHandler;

    public ReplayModRecording(ReplayMod mod) {
        core = mod;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, new Runnable() {
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

    @Override
    public void initClient() {
        FML_BUS.register(connectionEventHandler = new ConnectionEventHandler(LOGGER, core));

        new GuiHandler(core).register();

        //#if MC>=11300
        // FIXME
        //#else
        //$$ NetworkRegistry.INSTANCE.newChannel(Restrictions.PLUGIN_CHANNEL, new RestrictionsChannelHandler());
        //#endif
    }

    @ChannelHandler.Sharable
    private static class RestrictionsChannelHandler extends ChannelDuplexHandler {}

    public void initiateRecording(NetworkManager networkManager) {
        Channel channel = networkManager.channel();
        if (channel.pipeline().get("ReplayModReplay_replaySender") != null) return;
        if (channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }
}
