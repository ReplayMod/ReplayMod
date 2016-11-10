package com.replaymod.compat.shaders.mixin;

import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinShaderRenderGlobal {

    private final Minecraft mc = Minecraft.getMinecraft();

    @Shadow
    public boolean displayListEntitiesDirty;

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    public void replayModCompat_setupTerrain(Entity viewEntity, double partialTicks, ICamera camera,
                                             int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (((EntityRendererHandler.IEntityRenderer) mc.entityRenderer).replayModRender_getHandler() == null) return;
        if (ShaderReflection.shaders_isShadowPass == null) return;

        // when called by the shadow pass, displayListEntitiesDirty can't be set to false, as no chunk updates
        // are being processed. As it's being set to true by ChunkLoadingRenderGlobal#updateChunks, we have to
        // set it to false manually to exit the loop imposed by MixinRenderGlobal#replayModRender_setupTerrain.
        try {
            if ((boolean) ShaderReflection.shaders_isShadowPass.get(null) == true) {
                displayListEntitiesDirty = false;
            }
        } catch (IllegalAccessException ignore) {}

    }

}
