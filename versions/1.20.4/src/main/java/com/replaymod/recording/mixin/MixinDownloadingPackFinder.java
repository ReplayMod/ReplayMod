package com.replaymod.recording.mixin;

import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.packet.PacketListener;
import net.minecraft.client.resource.server.ServerResourcePackManager;
import net.minecraft.util.Downloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerResourcePackManager.class)
public abstract class MixinDownloadingPackFinder {
    @Inject(method = "onDownload", at = @At("HEAD"))
    private void recordDownloadedPack(@Coerce Object packs, Downloader.DownloadResult result, CallbackInfo ci) {
        PacketListener packetListener = ReplayModRecording.instance.getConnectionEventHandler().getPacketListener();
        if (packetListener == null) {
            return;
        }
        for (Map.Entry<UUID, Path> entry : result.downloaded().entrySet()) {
            packetListener.getResourcePackRecorder().recordResourcePack(entry.getValue(), entry.getKey());
        }
    }
}
