//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ModelRendererExporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11300
import net.minecraft.client.model.Cuboid;
//#else
//$$ import net.minecraft.client.model.ModelRenderer;
//#endif

@Mixin(Cuboid.class)
public abstract class MixinModelRenderer {
    @Inject(method = "render", at = @At("HEAD"))
    public void preRender(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).preRenderModel((Cuboid)(Object)this, scale);
        }
    }

    @Inject(method = "method_2852", at = @At("HEAD"))
    public void preRenderWithRotation(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).preRenderModel((Cuboid)(Object)this, scale);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void postRender(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).postRenderModel();
        }
    }

    @Inject(method = "method_2852", at = @At("RETURN"))
    public void postRenderWithRotation(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).postRenderModel();
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lcom/mojang/blaze3d/platform/GlStateManager;callList(I)V"),
            expect = 3)
    public void onRender(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).onRenderModel();
        }
    }

    @Inject(method = "method_2852",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;callList(I)V"))
    public void onRenderWithRotation(float scale, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ModelRendererExporter.class).onRenderModel();
        }
    }
}
//#endif
