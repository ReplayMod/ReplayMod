package com.replaymod.recording;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.recording.handler.GuiHandler;
import com.replaymod.recording.mixin.NetworkManagerAccessor;
import com.replaymod.recording.packet.PacketListener;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.ClientConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11400
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
//#else
//$$ import net.minecraftforge.fml.network.NetworkRegistry;
//#endif

//#if MC>=11300
//#else
//$$ import io.netty.channel.ChannelDuplexHandler;
//$$ import io.netty.channel.ChannelHandler;
//#endif

public class ReplayModRecording implements Module {

    private static final Logger LOGGER = LogManager.getLogger();
    //#if MC>=11300
    private static final AttributeKey<Void> ATTR_CHECKED = AttributeKey.newInstance("ReplayModRecording_checked");
    //#endif

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
                    packetListener.addMarker(null);
                    core.printInfoToChat("replaymod.chat.addedmarker");
                }
            }
        });
    }

    @Override
    public void initClient() {
        connectionEventHandler = new ConnectionEventHandler(LOGGER, core);

        new GuiHandler(core).register();

        //#if MC>=11400
        ClientSidePacketRegistry.INSTANCE.register(Restrictions.PLUGIN_CHANNEL, (packetContext, packetByteBuf) -> {});
        //#else
        //#if MC>=11300
        //$$ NetworkRegistry.newEventChannel(Restrictions.PLUGIN_CHANNEL, () -> "0", any -> true, any -> true);
        //#else
        //$$ NetworkRegistry.INSTANCE.newChannel(Restrictions.PLUGIN_CHANNEL, new RestrictionsChannelHandler());
        //#endif
        //#endif
    }

    //#if MC<11300
    //$$ @ChannelHandler.Sharable
    //$$ private static class RestrictionsChannelHandler extends ChannelDuplexHandler {}
    //#endif

    public void initiateRecording(ClientConnection networkManager) {
        Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
        if (channel.pipeline().get("ReplayModReplay_replaySender") != null) return;
        //#if MC>=11300
        if (channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        //#endif
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }

    public ConnectionEventHandler getConnectionEventHandler() {
        return connectionEventHandler;
    }
}
