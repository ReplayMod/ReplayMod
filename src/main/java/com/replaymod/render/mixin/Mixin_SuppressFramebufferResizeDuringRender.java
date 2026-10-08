package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.gui.progress.VirtualWindow;
import com.replaymod.render.hooks.WindowDelegateHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class Mixin_SuppressFramebufferResizeDuringRender implements WindowDelegateHolder {

    @Unique
    private VirtualWindow windowDelegate;

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.windowDelegate = window;
    }

    //#if MC>=26.3
    //$$ // 26.3 renamed MinecraftClient#onResolutionChanged to #framebufferSizeChanged
    //$$ @Inject(method = "framebufferSizeChanged", at = @At("HEAD"), cancellable = true)
    //#elseif MC>=11400
    @Inject(method = "onResolutionChanged", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    //#endif
    private void suppressResizeDuringRender(CallbackInfo ci) {
        VirtualWindow delegate = this.windowDelegate;
        if (delegate != null && (delegate.isBound() || MCVer.syntheticFramebufferResize)) {
            // Synthetic resizes (video resolution, see MCVer#resizeMainWindow) only exist to render the video
            // frames at a different resolution. The GUI must keep using the actual window size at all times,
            // otherwise the rendered GUI (video-based scale) desyncs from the interactive layout (window-based
            // scale) during rendering. Real window resizes are forwarded to the virtual GUI window so it can
            // adapt to the new window size.
            if (delegate.isBound() && !MCVer.syntheticFramebufferResize) {
                Window window = ((MinecraftClient) (Object) this).getWindow();
                delegate.onResolutionChanged(window.getFramebufferWidth(), window.getFramebufferHeight());
            }
            ci.cancel();
        }
    }
}
