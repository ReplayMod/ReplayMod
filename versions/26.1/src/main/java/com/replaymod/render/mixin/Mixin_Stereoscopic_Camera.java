package com.replaymod.render.mixin;

import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mixin(Camera.class)
public abstract class Mixin_Stereoscopic_Camera {
    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void replayModRender_setupStereoscopicProjection(CallbackInfo ci) {
        if (getHandler() == null || !(getHandler().data instanceof StereoscopicOpenGlFrameCapturer.Data)) return;
        StereoscopicOpenGlFrameCapturer.Data data = (StereoscopicOpenGlFrameCapturer.Data) getHandler().data;

        move(0f, 0f, 0.1f * (data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? -1 : 1));
    }

    @Unique
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }
}
