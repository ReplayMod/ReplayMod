//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10904
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#else
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#endif

//#if MC>=10904
@Mixin(RenderLivingBase.class)
//#else
//$$ @Mixin(RendererLivingEntity.class)
//#endif
public abstract class MixinRenderLivingBase {
    @Inject(method = "doRender", at = @At(
            value = "INVOKE",
            //#if MC>=10904
            target = "Lnet/minecraft/client/renderer/entity/RenderLivingBase;prepareScale(Lnet/minecraft/entity/EntityLivingBase;F)F",
            //#else
            //$$ target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;preRenderCallback(Lnet/minecraft/entity/EntityLivingBase;F)V",
            //#endif
            shift = At.Shift.AFTER
    ))
    private void recordModelMatrix(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postEntityLivingSetup();
        }
    }
}
//#endif
