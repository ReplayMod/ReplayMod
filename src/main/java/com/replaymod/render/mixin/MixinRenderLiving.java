package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLiving.class)
public abstract class MixinRenderLiving {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void replayModRender_areAllNamesHidden(EntityLiving entity, CallbackInfoReturnable<Boolean> ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && !handler.getSettings().isRenderNameTags()) {
            ci.setReturnValue(false); //this calls the cancel method
        }
    }
}
