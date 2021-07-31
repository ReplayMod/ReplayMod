package com.replaymod.recording.mixin;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;

@Mixin({ S21PacketChunkData.class, S26PacketMapChunkBulk.class })
public abstract class MixinS26PacketMapChunkBulk {

    private byte[] rawData;

    @Shadow(remap = false)
    private Semaphore deflateGate;

    @Inject(method = "readPacketData", at = @At("HEAD"))
    private void replayModRecording_readRawPacketData(PacketBuffer data, CallbackInfo ci) {
        int readerIndex = data.readerIndex();
        data.readBytes(rawData = new byte[data.readableBytes()]);
        data.readerIndex(readerIndex);
    }

    @Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
    private void replayModRecording_writePacketData(PacketBuffer data, CallbackInfo ci) {
        if (rawData != null && deflateGate == null) {
            data.writeBytes(rawData);
            ci.cancel();
        }
    }
}
