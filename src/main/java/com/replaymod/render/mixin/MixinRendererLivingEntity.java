package com.replaymod.render.mixin;

//#if MC<10904
//$$ import com.replaymod.render.hooks.EntityRendererHandler;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//$$ import net.minecraft.entity.EntityLivingBase;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Mixin(RendererLivingEntity.class)
//$$ public abstract class MixinRendererLivingEntity {
//$$     @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
//$$     private void replayModRender_areAllNamesHidden(EntityLivingBase entity, CallbackInfoReturnable<Boolean> ci) {
//$$         EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
//$$         if (handler != null && !handler.getSettings().isRenderNameTags()) {
//$$             ci.setReturnValue(false); //this calls the cancel method
//$$         }
//$$     }
//$$ }
//#endif
