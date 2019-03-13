//#if MC>=11300
package com.replaymod.recording.mixin;

import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.recording.packet.ResourcePackRecorder;
import de.johni0702.minecraft.gui.utils.Consumer;
import net.minecraft.client.resources.DownloadingPackFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(DownloadingPackFinder.class)
public abstract class MixinDownloadingPackFinder implements ResourcePackRecorder.IDownloadingPackFinder {
    private Consumer<File> requestCallback;

    @Override
    public void setRequestCallback(Consumer<File> callback) {
        requestCallback = callback;
    }

    @Shadow
    public abstract ListenableFuture<Object> func_195741_a(File file);

    @Redirect(method = "downloadResourcePack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/DownloadingPackFinder;func_195741_a(Ljava/io/File;)Lcom/google/common/util/concurrent/ListenableFuture;"))
    private ListenableFuture<Object> recordDownloadedPack(DownloadingPackFinder downloadingPackFinder, File file) {
        if (requestCallback != null) {
            requestCallback.consume(file);
            requestCallback = null;
        }
        return func_195741_a(file);
    }
}
//#endif
