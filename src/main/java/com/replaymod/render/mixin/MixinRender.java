package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
public abstract class MixinRender {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void replayModRender_areAllNamesHidden(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && !handler.getSettings().isRenderNameTags()) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }
}
