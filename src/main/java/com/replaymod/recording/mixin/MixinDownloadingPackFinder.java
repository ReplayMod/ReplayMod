//#if MC>=10800
package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ResourcePackRecorder;
import de.johni0702.minecraft.gui.utils.Consumer;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

//#if MC>=11600
//$$ import net.minecraft.resource.ResourcePackSource;
//#endif

//#if MC>=10800
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import net.minecraft.util.HttpUtil;
//$$ import java.lang.reflect.Proxy;
//$$ import java.util.Map;
//#endif

@Mixin(ClientBuiltinResourcePackProvider.class)
public abstract class MixinDownloadingPackFinder implements ResourcePackRecorder.IDownloadingPackFinder {
    private Consumer<File> requestCallback;

    @Override
    public void setRequestCallback(Consumer<File> callback) {
        requestCallback = callback;
    }

    //#if MC>=10800
    @Inject(method = "loadServerPack", at = @At("HEAD"))
    private void recordDownloadedPack(
            File file,
            //#if MC>=11600
            //$$ ResourcePackSource arg,
            //#endif
            CallbackInfoReturnable ci
    ) {
        if (requestCallback != null) {
            requestCallback.consume(file);
            requestCallback = null;
        }
    }
    //#else
    //$$ @Redirect(method = "func_148528_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/HttpUtil;downloadResourcePack(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/util/HttpUtil$DownloadListener;Ljava/util/Map;ILnet/minecraft/util/HttpUtil$IProgressUpdate;Ljava/net/Proxy;)V"))
    //$$ private void downloadResourcePack(File dst, String url, HttpUtil.DownloadListener callback, final Map headers, final int maxSize, final HttpUtil.IProgressUpdate progress, Proxy proxy) {
    //$$     HttpUtil.downloadResourcePack(dst, url, new HttpUtil.DownloadListener() {
    //$$         public void onDownloadComplete(File file) {
    //$$             if (requestCallback != null) {
    //$$                 requestCallback.consume(file);
    //$$                 requestCallback = null;
    //$$             }
    //$$             callback.onDownloadComplete(file);
    //$$         }
    //$$     }, headers, maxSize, progress, proxy);
    //$$ }
    //#endif
}
//#endif
