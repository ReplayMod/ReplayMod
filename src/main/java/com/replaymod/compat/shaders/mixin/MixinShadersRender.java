package com.replaymod.compat.shaders.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "shadersmod/client/ShadersRender", // Pre Optifine 1.12.2 E1
        "net/optifine/shaders/ShadersRender" // Post Optifine 1.12.2 E1
}, remap = false)
public abstract class MixinShadersRender {

    @Inject(method = "renderHand0", at = @At("HEAD"), cancellable = true)
    private static void replayModCompat_disableRenderHand0(GameRenderer er, float partialTicks, int renderPass, CallbackInfo ci) {
        //#if MC>=11300
        if (ForgeHooksClient.renderFirstPersonHand(er.getMinecraft().renderGlobal, partialTicks)) {
        //#else
        //$$ if (ForgeHooksClient.renderFirstPersonHand(er.mc.renderGlobal, partialTicks, renderPass)) {
        //#endif
            ci.cancel();
        }
    }

}
