//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//#if MC>=10904
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {

    @Inject(method = "doRenderEntity",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V"))
    public void preRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks, boolean box,
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

    @Inject(method = "doRenderEntity",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V",
                     shift = At.Shift.AFTER))
    public void postRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks, boolean box,
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
