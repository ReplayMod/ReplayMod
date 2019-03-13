package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=10800
import net.minecraft.client.renderer.culling.Frustum;
//#else
//$$ import net.minecraft.client.renderer.culling.Frustrum;
//#endif

//#if MC>=10800
@Mixin(Frustum.class)
//#else
//$$ @Mixin(Frustrum.class)
//#endif
public abstract class MixinFrustum {
    @Inject(method = "isBoxInFrustum", at = @At("HEAD"), cancellable = true)
    public void isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, CallbackInfoReturnable<Boolean> ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional && handler.data == null) {
            // Normally the camera is always facing the direction of the omnidirectional image face that is currently
            // getting rendered. With ODS however, the camera is always facing forwards and the turning happens in the
            // vertex shader (non-trivial due to stereo). As such, all chunks need to be rendered all the time for ODS.
            ci.setReturnValue(true);
        }
    }
}
