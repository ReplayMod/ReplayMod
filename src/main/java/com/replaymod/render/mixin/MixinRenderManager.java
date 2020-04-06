package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//#if MC>=11500
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Quaternion;
//#endif

//#if MC>=10904
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinRenderManager {
    //#if MC>=11500
    @Shadow private Quaternion rotation;
    //#else
    //$$ @Shadow private float cameraPitch;
    //$$ @Shadow private float cameraYaw;
    //#endif

    //#if MC>=11500
    @Inject(method = "render", at = @At("HEAD"))
    //#else
    //#if MC>=11400 && FABRIC>=1
    //$$ @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)V", at = @At("HEAD"))
    //#else
    //#if MC>=11400
    //$$ @Inject(method = "renderEntity", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "doRenderEntity", at = @At("HEAD"))
    //#endif
    //#endif
    //#endif
    //#if MC>=10904
    private void replayModRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz, float iDoNotKnow, float partialTicks,
                                                           //#if MC>=11500
                                                           MatrixStack matrixStack,
                                                           VertexConsumerProvider vertexConsumerProvider,
                                                           int int_1,
                                                           //#else
                                                           //$$ boolean iDoNotCare,
                                                           //#endif
                                                           CallbackInfo ci) {
    //#else
    //$$ private void replayModRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz, float iDoNotKnow, float partialTicks, boolean iDoNotCare, CallbackInfoReturnable<Boolean> ci) {
    //#endif
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);
            //#if MC>=11500
            this.rotation = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
            this.rotation.hamiltonProduct(Vector3f.POSITIVE_Y.getDegreesQuaternion((float) -yaw));
            this.rotation.hamiltonProduct(Vector3f.POSITIVE_X.getDegreesQuaternion((float) pitch));
            //#else
            //$$ this.cameraPitch = (float) Math.toDegrees(pitch);
            //$$ this.cameraYaw = (float) Math.toDegrees(yaw);
            //#endif
        }
    }
}
