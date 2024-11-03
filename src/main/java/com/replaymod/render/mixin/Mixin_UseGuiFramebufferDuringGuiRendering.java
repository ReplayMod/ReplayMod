package com.replaymod.render.mixin;

import com.replaymod.render.hooks.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class Mixin_UseGuiFramebufferDuringGuiRendering implements MinecraftClientExt {

    @Unique
    private Framebuffer framebufferDelegate;

    @Override
    public void setFramebufferDelegate(Framebuffer framebuffer) {
        this.framebufferDelegate = framebuffer;
    }

    @Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
    private void useGuiFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
        Framebuffer delegate = this.framebufferDelegate;
        if (delegate != null) {
            ci.setReturnValue(delegate);
        }
    }
}
