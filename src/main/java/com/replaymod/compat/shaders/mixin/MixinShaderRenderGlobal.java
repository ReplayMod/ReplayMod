//#if MC>=10800
package com.replaymod.compat.shaders.mixin;

import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ICamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import net.minecraft.client.render.Camera;
//#else
import net.minecraft.entity.Entity;
//#endif

@Mixin(WorldRenderer.class)
public abstract class MixinShaderRenderGlobal {

    private final Minecraft mc = Minecraft.getInstance();

    @Shadow
    public boolean displayListEntitiesDirty;

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    public void replayModCompat_setupTerrain(
            //#if MC>=11400
            //$$ Camera viewEntity,
            //#else
            Entity viewEntity,
            //#if MC>=11300
            float partialTicks,
            //#else
            //$$ double partialTicks,
            //#endif
            //#endif
            ICamera camera,
            int frameCount,
            boolean playerSpectator,
            CallbackInfo ci
    ) {
        if (((EntityRendererHandler.IEntityRenderer) mc.entityRenderer).replayModRender_getHandler() == null) return;
        if (ShaderReflection.shaders_isShadowPass == null) return;

        // when called by the shadow pass, displayListEntitiesDirty can't be set to false, as no chunk updates
        // are being processed. As it's being set to true by ChunkLoadingRenderGlobal#updateChunks, we have to
        // set it to false manually to exit the loop imposed by MixinRenderGlobal#replayModRender_setupTerrain.
        try {
            if ((boolean) ShaderReflection.shaders_isShadowPass.get(null) == true) {
                this.displayListEntitiesDirty = false;
            }
        } catch (IllegalAccessException ignore) {}

    }

}
//#endif
