//#if MC>=10800
package com.replaymod.render.mixin;

import com.replaymod.render.hooks.FogStateCallback;
import com.replaymod.render.hooks.Texture2DStateCallback;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
    @Shadow private static int activeTexture;

    @Inject(method = "enableFog", at = @At("HEAD"))
    private static void enableFog(CallbackInfo ci) {
        FogStateCallback.EVENT.invoker().fogStateChanged(true);
    }

    @Inject(method = "disableFog", at = @At("HEAD"))
    private static void disableFog(CallbackInfo ci) {
        FogStateCallback.EVENT.invoker().fogStateChanged(false);
    }

    @Inject(method = "enableTexture2D", at = @At("HEAD"))
    private static void enableTexture2D(CallbackInfo ci) {
        Texture2DStateCallback.EVENT.invoker().texture2DStateChanged(MixinGlStateManager.activeTexture, true);
    }

    @Inject(method = "disableTexture2D", at = @At("HEAD"))
    private static void disableTexture2D(CallbackInfo ci) {
        Texture2DStateCallback.EVENT.invoker().texture2DStateChanged(MixinGlStateManager.activeTexture, false);
    }
}
//#endif
