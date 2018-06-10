package com.replaymod.restrictions;

import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.utils.Restrictions;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

//#if MC>=10800
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
//#else
//$$ import cpw.mods.fml.common.FMLCommonHandler;
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.event.FMLInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPostInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//$$ import cpw.mods.fml.common.eventhandler.EventBus;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.PlayerEvent;
//$$ import cpw.mods.fml.common.network.FMLEmbeddedChannel;
//$$ import cpw.mods.fml.common.network.FMLOutboundHandler;
//$$ import cpw.mods.fml.common.network.NetworkRegistry;
//$$ import cpw.mods.fml.common.network.internal.FMLProxyPacket;
//$$ import cpw.mods.fml.relauncher.Side;
//#endif

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

@Mod(modid = ReplayModRestrictions.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        serverSideOnly = true,
        //#endif
        useMetadata = true)
public class ReplayModRestrictions {
    public static final String MOD_ID = "replaymod-restrictions";

    public static Logger LOGGER;

    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    private FMLEmbeddedChannel channel;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();

        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        settingsRegistry.setConfiguration(config);

        settingsRegistry.register(Setting.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        //#if MC<=10710
        //$$ if (FMLCommonHandler.instance().getSide() == Side.CLIENT) return;
        //#endif
        EventBus bus = FML_BUS;
        bus.register(this);

        channel = NetworkRegistry.INSTANCE.newChannel(Restrictions.PLUGIN_CHANNEL, new RestrictionsChannelHandler()).get(Side.SERVER);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
        settingsRegistry.save(); // Save default values to disk
    }

    @ChannelHandler.Sharable
    private static class RestrictionsChannelHandler extends ChannelDuplexHandler {}

    @SubscribeEvent
    public void onUserJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Map<String, byte[]> restrictions = new HashMap<>();
        if (settingsRegistry.get(Setting.NO_XRAY)) restrictions.put("no_xray", new byte[0]);
        if (settingsRegistry.get(Setting.NO_NOCLIP)) restrictions.put("no_noclip", new byte[0]);
        if (settingsRegistry.get(Setting.ONLY_FIRST_PERSON)) restrictions.put("only_first_person", new byte[0]);
        if (settingsRegistry.get(Setting.ONLY_RECORDING_PLAYER)) restrictions.put("only_recording_player", new byte[0]);
        if (settingsRegistry.get(Setting.HIDE_COORDINATES)) restrictions.put("hide_coordinates", new byte[0]);

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        for (Map.Entry<String, byte[]> e : restrictions.entrySet()) {
            byte[] bytes = e.getKey().getBytes();
            buf.writeByte(bytes.length);
            buf.writeBytes(bytes);
            buf.writeByte(1);
            buf.writeBytes(e.getValue());
        }

        FMLProxyPacket packet = new FMLProxyPacket(buf, Restrictions.PLUGIN_CHANNEL);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(event.player);
        channel.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
