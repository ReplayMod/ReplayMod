package com.replaymod.render.mixin;

import com.replaymod.render.gui.progress.VirtualWindow;
import com.replaymod.render.hooks.MinecraftClientExt;
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
public class Mixin_SuppressFramebufferResizeDuringRender implements MinecraftClientExt {

    @Unique
    private VirtualWindow windowDelegate;

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.windowDelegate = window;
    }

    //#if MC>=11400
    @Inject(method = "onResolutionChanged", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    //#endif
    private void suppressResizeDuringRender(CallbackInfo ci) {
        VirtualWindow delegate = this.windowDelegate;
        if (delegate != null && delegate.isBound()) {
            Window window = ((MinecraftClient) (Object) this).getWindow();
            delegate.onResolutionChanged(window.getFramebufferWidth(), window.getFramebufferHeight());
            ci.cancel();
        }
    }
}
