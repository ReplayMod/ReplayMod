package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gl.FramebufferManager;
import net.minecraft.client.gl.GlResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlResourceManager.class)
public class Mixin_FixCopyTextureToBuffer_DepthTexture {
    @WrapOperation(
            method = "copyTextureToBuffer(Lcom/mojang/blaze3d/textures/GpuTexture;Lcom/mojang/blaze3d/buffers/GpuBuffer;ILjava/lang/Runnable;IIIII)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/FramebufferManager;setupFramebuffer(IIIII)V")
    )
    private void setupFramebuffer(
            FramebufferManager instance,
            int framebuffer, int colorAttachment, int depthAttachment, int mipLevel, int bindTarget,
            Operation<Void> original,
            @Local(argsOnly = true) GpuTexture texture
    ) {
        if (depthAttachment == 0 && texture.getFormat().hasDepthAspect()) {
            depthAttachment = colorAttachment;
            colorAttachment = 0;
        }
        original.call(instance, framebuffer, colorAttachment, depthAttachment, mipLevel, bindTarget);
    }
}
