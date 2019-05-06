//#if MC>=10800
package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ResourcePackRecorder;
import de.johni0702.minecraft.gui.utils.Consumer;
import net.minecraft.client.resources.DownloadingPackFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

//#if MC>=11400
//$$ import java.util.concurrent.CompletableFuture;
//#else
import com.google.common.util.concurrent.ListenableFuture;
//#endif

@Mixin(DownloadingPackFinder.class)
public abstract class MixinDownloadingPackFinder implements ResourcePackRecorder.IDownloadingPackFinder {
    private Consumer<File> requestCallback;

    @Override
    public void setRequestCallback(Consumer<File> callback) {
        requestCallback = callback;
    }

    @Shadow
    public abstract
    //#if MC>=11400
    //$$ CompletableFuture<Object>
    //#else
    ListenableFuture<Object>
    //#endif
    func_195741_a(File file);

    //#if MC>=11400
    //$$ @Redirect(method = "download", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/ClientResourcePackCreator;loadServerPack(Ljava/io/File;)Ljava/util/concurrent/CompletableFuture;"))
    //$$ private CompletableFuture<Object>
    //#else
    //#if MC>=10800
    @Redirect(method = "downloadResourcePack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/DownloadingPackFinder;func_195741_a(Ljava/io/File;)Lcom/google/common/util/concurrent/ListenableFuture;"))
    //#else
    //$$ @Redirect(method = "func_180601_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/ResourcePackRepository;func_177319_a(Ljava/io/File;)Lcom/google/common/util/concurrent/ListenableFuture;"))
    //#endif
    private ListenableFuture<Object>
    //#endif
    recordDownloadedPack(DownloadingPackFinder downloadingPackFinder, File file) {
        if (requestCallback != null) {
            requestCallback.consume(file);
            requestCallback = null;
        }
        return func_195741_a(file);
    }
}
//#endif
