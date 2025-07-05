package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public abstract class Mixin_ChromaKeyDisableFog {
    @ModifyVariable(method = "getFogBuffer", at = @At("HEAD"), argsOnly = true)
    private FogRenderer.FogType replayModRender_suppressFogIfChromaKeyIsActive(FogRenderer.FogType fogType) {
        MinecraftClient mc = MCVer.getMinecraft();
        if (mc == null) return fogType;
        GameRenderer gameRenderer = mc.gameRenderer;
        if (gameRenderer == null) return fogType;
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.getSettings().getChromaKeyingColor() != null) {
            fogType = FogRenderer.FogType.NONE;
        }
        return fogType;
    }
}
