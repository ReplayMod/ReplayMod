package com.replaymod.recording.mixin;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkDispatcher.class, remap = false)
public abstract class MixinNetworkDispatcher {

    @Shadow
    private EmbeddedChannel handshakeChannel;

    /**
     * Always sets fml:isLocal to false.
     * This effectively removes the difference in the FML handshake between SP and MP
     * and forces the block/item ids, etc. to always be send.
     * This might have undesired side effects but at least it works at all.
     */
    @Inject(method = "insertIntoChannel", at=@At("HEAD"))
    public void replayModRecording_forceIsLocalToFalse(CallbackInfo cb) {
        handshakeChannel.attr(NetworkDispatcher.IS_LOCAL).set(false);
    }
}
