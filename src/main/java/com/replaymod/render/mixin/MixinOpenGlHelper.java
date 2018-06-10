//#if MC<=10710
//$$ package com.replaymod.render.mixin;
//$$
//$$ import com.replaymod.render.hooks.GLStateTracker;
//$$ import net.minecraft.client.renderer.OpenGlHelper;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Mixin(OpenGlHelper.class)
//$$ public abstract class MixinOpenGlHelper {
//$$     @Inject(method = "setActiveTexture", at = @At("RETURN"))
//$$     private static void replayModRender_trackActiveTexture(int magic, CallbackInfo ci) {
//$$         GLStateTracker.getInstance().updateActiveTexture(magic);
//$$     }
//$$ }
//#endif
