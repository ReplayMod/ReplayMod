package com.replaymod.render.mixin;

import com.replaymod.render.hooks.FramebufferDelegateHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC >= 26.2
//$$ import net.minecraft.client.renderer.GameRenderer;
//#endif

//#if MC >= 26.2
//$$ @Mixin(GameRenderer.class)
//#else
@Mixin(MinecraftClient.class)
//#endif
public abstract class Mixin_UseGuiFramebufferDuringGuiRendering implements FramebufferDelegateHolder {

    @Unique
    private Framebuffer framebufferDelegate;

    @Override
    public void setFramebufferDelegate(Framebuffer framebuffer) {
        this.framebufferDelegate = framebuffer;
    }

    //#if MC >= 26.2
    //$$ @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
    //#endif
    private void useGuiFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
        Framebuffer delegate = this.framebufferDelegate;
        if (delegate != null) {
            ci.setReturnValue(delegate);
        }
    }
}
