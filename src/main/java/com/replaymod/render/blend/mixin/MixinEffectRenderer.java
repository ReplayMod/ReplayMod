//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ParticlesExporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#else
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC>=10904
import net.minecraft.client.particle.ParticleManager;
//#else
//$$ import net.minecraft.client.particle.EffectRenderer;
//#endif

//#if MC>=10904
@Mixin(ParticleManager.class)
//#else
//$$ @Mixin(EffectRenderer.class)
//#endif
public abstract class MixinEffectRenderer {

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void preRender(
            //#if MC>=11400
            Camera view,
            //#else
            //$$ Entity view,
            //#endif
            float renderPartialTicks,
            CallbackInfo ci
    ) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ParticlesExporter.class).preParticlesRender(false);
        }
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void postRender(
            //#if MC>=11400
            Camera view,
            //#else
            //$$ Entity view,
            //#endif
            float renderPartialTicks,
            CallbackInfo ci
    ) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ParticlesExporter.class).postParticlesRender();
        }
    }

    //#if MC<11400
    //$$ @Inject(method = "renderLitParticles", at = @At("HEAD"))
    //$$ public void preLitRender(Entity view, float renderPartialTicks, CallbackInfo ci) {
    //$$     BlendState blendState = BlendState.getState();
    //$$     if (blendState != null) {
    //$$         blendState.get(ParticlesExporter.class).preParticlesRender(true);
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "renderLitParticles", at = @At("RETURN"))
    //$$ public void postLitRender(Entity view, float renderPartialTicks, CallbackInfo ci) {
    //$$     BlendState blendState = BlendState.getState();
    //$$     if (blendState != null) {
    //$$         blendState.get(ParticlesExporter.class).postParticlesRender();
    //$$     }
    //$$ }
    //#endif
}
//#endif
