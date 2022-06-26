package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.PacketListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {
    @Shadow
    private Channel channel;

    @Inject(method = "setCompressionThreshold", at = @At("RETURN"))
    private void ensureReplayModRecorderIsAfterDecompress(CallbackInfo ci) {
        ChannelHandler recorder = null;
        for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
            String key = entry.getKey();
            if (PacketListener.RAW_RECORDER_KEY.equals(key)) {
                recorder = entry.getValue();
            }
            if (PacketListener.DECOMPRESS_KEY.equals(key)) {
                if (recorder != null) {
                    // If we've already found the recorder, then that means decompress is after recorder. That's no good
                    // because it means the recorder is getting compressed packets, we need to move the recorder.
                    channel.pipeline().remove(recorder);
                    channel.pipeline().addBefore(PacketListener.DECODER_KEY, PacketListener.RAW_RECORDER_KEY, recorder);
                    return;
                }
            }
        }
    }
}
