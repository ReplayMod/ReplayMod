//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//#if MC>=11500
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=10904
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinRenderManager {

    @Inject(
            //#if MC>=11500
            //$$ method = "render",
            //#else
            //#if MC>=11400 && FABRIC>=1
            method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)V",
            //#else
            //#if MC>=11400
            //$$ method = "renderEntity",
            //#else
            //$$ method = "doRenderEntity",
            //#endif
            //#endif
            //#endif
            at = @At(value = "INVOKE",
                     //#if MC>=11500
                     //$$ target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
                     //#else
                     target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;DDDFF)V"))
                     //#endif
    public void preRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks,
                          //#if MC>=11500
                          //$$ MatrixStack matrixStack,
                          //$$ VertexConsumerProvider vertexConsumerProvider,
                          //$$ int int_1,
                          //#else
                          boolean box,
                          //#endif
                          //#if MC>=10904
                          CallbackInfo ci) {
                          //#else
                          //$$ CallbackInfoReturnable<Boolean> ci) {
                          //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).preRender(entity, x, y, z, yaw, renderPartialTicks);
        }
    }

    @Inject(
            //#if MC>=11500
            //$$ method = "render",
            //#else
            //#if MC>=11400
            method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)V",
            //#else
            //#if MC>=11400
            //$$ method = "renderEntity",
            //#else
            //$$ method = "doRenderEntity",
            //#endif
            //#endif
            //#endif
            at = @At(value = "INVOKE",
                     //#if MC>=11500
                     //$$ target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                     //#else
                     target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;DDDFF)V",
                     //#endif
                     shift = At.Shift.AFTER))
    public void postRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks,
                           //#if MC>=11500
                           //$$ MatrixStack matrixStack,
                           //$$ VertexConsumerProvider vertexConsumerProvider,
                           //$$ int int_1,
                           //#else
                           boolean box,
                           //#endif
                           //#if MC>=10904
                           CallbackInfo ci) {
                           //#else
                           //$$ CallbackInfoReturnable<Boolean> ci) {
                           //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postRender(entity, x, y, z, yaw, renderPartialTicks);
        }
    }
}
//#endif
