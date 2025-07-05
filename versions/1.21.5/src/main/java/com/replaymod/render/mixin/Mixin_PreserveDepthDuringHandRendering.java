package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class Mixin_PreserveDepthDuringHandRendering {
    @WrapWithCondition(
            method = "renderWorld",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V")
    )
    private boolean replayModRender_skipClearWhenRecordingDepth(CommandEncoder instance, GpuTexture texture, double v) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this).replayModRender_getHandler();
        return handler == null || !handler.getSettings().isDepthMap();
    }
}
