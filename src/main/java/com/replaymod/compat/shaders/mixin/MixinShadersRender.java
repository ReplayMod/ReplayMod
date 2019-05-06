package com.replaymod.compat.shaders.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import com.replaymod.core.events.PreRenderHandCallback;
//#else
import com.replaymod.core.versions.MCVer;
import net.minecraftforge.client.ForgeHooksClient;
//#endif

@Pseudo
@Mixin(targets = {
        "shadersmod/client/ShadersRender", // Pre Optifine 1.12.2 E1
        "net/optifine/shaders/ShadersRender" // Post Optifine 1.12.2 E1
}, remap = false)
public abstract class MixinShadersRender {

    @Inject(method = "renderHand0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replayModCompat_disableRenderHand0(GameRenderer er, float partialTicks, int renderPass, CallbackInfo ci) {
        //#if MC>=11400
        //$$ if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
        //#else
        //#if MC>=11300
        if (ForgeHooksClient.renderFirstPersonHand(MCVer.getMinecraft().renderGlobal, partialTicks)) {
        //#else
        //$$ if (ForgeHooksClient.renderFirstPersonHand(MCVer.getMinecraft().renderGlobal, partialTicks, renderPass)) {
        //#endif
        //#endif
            ci.cancel();
        }
    }

}
