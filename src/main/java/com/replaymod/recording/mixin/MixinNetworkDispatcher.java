package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.FMLHandshakeFilter;
import cpw.mods.fml.common.network.handshake.FMLHandshakeCodec;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkDispatcher.class, remap = false)
public abstract class MixinNetworkDispatcher {

    @Shadow
    private Side side;

    @Shadow
    private EmbeddedChannel handshakeChannel;

    /**
     * Always sets fml:isLocal to false on the server side.
     * This effectively removes the difference in the FML handshake between SP and MP
     * and forces the block/item ids, etc. to always be send.
     * Injects a {@link FMLHandshakeFilter} on the client side to filter out
     * those extra, unexpected packets.
     */
    @Inject(method = "insertIntoChannel", at=@At("HEAD"))
    public void replayModRecording_setupForLocalRecording(CallbackInfo cb) {
        // If we're in multiplayer, everything is fine as is
        if (!handshakeChannel.attr(NetworkDispatcher.IS_LOCAL).get()) return;

        if (side == Side.SERVER) {
            // On the server side, force all packets to be sent
            handshakeChannel.attr(NetworkDispatcher.IS_LOCAL).set(false);
        } else {
            // On the client side, discard additional packets
            ChannelPipeline pipeline = handshakeChannel.pipeline();
            pipeline.addAfter(pipeline.context(FMLHandshakeCodec.class).name(),
                    "replaymod_filter", new FMLHandshakeFilter());
        }
    }
}
