package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class Mixin_Omnidirectional_Camera implements EntityRendererHandler.IEntityRenderer {
    private static final String METHOD = "getBasicProjectionMatrix";
    //#if MC>=11903
    //#if MC>=12005
    //$$ private static final String TARGET = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;";
    //#else
    //$$ private static final String TARGET = "Lorg/joml/Matrix4f;setPerspective(FFFF)Lorg/joml/Matrix4f;";
    //#endif
    //$$ private static final boolean TARGET_REMAP = false;
    //$$ private static final float OMNIDIRECTIONAL_FOV = (float) Math.PI / 2;
    //#else
    private static final String TARGET = "Lnet/minecraft/util/math/Matrix4f;viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;";
    private static final boolean TARGET_REMAP = true;
    private static final float OMNIDIRECTIONAL_FOV = 90;
    //#endif

    @ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = TARGET, remap = TARGET_REMAP), index = 0)
    //#if MC>=11903
    //$$ private float replayModRender_perspective_fov(float fovY) {
    //#else
    private double replayModRender_perspective_fov(double fovY) {
    //#endif
        return isOmnidirectional() ? OMNIDIRECTIONAL_FOV : fovY;
    }

    @ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = TARGET, remap = TARGET_REMAP), index = 1)
    private float replayModRender_perspective_aspect(float aspect) {
        return isOmnidirectional() ? 1 : aspect;
    }

    @Unique
    private boolean isOmnidirectional() {
        return replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional;
    }
}
