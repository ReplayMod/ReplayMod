//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10904
import net.minecraft.client.render.entity.LivingEntityRenderer;
//#else
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#endif

//#if MC>=10904
@Mixin(LivingEntityRenderer.class)
//#else
//$$ @Mixin(RendererLivingEntity.class)
//#endif
public abstract class MixinRenderLivingBase {
    //#if MC>=11500
    //$$ @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(
    //#else
    //#if FABRIC>=1
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At(
    //#else
    //$$ @Inject(method = "doRender(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At(
    //#endif
    //#endif
            value = "INVOKE",
            //#if MC>=10904
            target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;scaleAndTranslate(Lnet/minecraft/entity/LivingEntity;F)F",
            //#else
            //$$ target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;preRenderCallback(Lnet/minecraft/entity/EntityLivingBase;F)V",
            //#endif
            shift = At.Shift.AFTER
    ))
    private void recordModelMatrix(CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postEntityLivingSetup();
        }
    }
}
//#endif
