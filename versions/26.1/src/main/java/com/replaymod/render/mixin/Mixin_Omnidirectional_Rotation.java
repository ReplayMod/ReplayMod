package com.replaymod.render.mixin;

import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//#if MC>=260200
//$$ import org.joml.Vector3fc;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=12005
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

//#if MC>=11500
//#else
//$$ import org.lwjgl.opengl.GL11;
//#endif

import static com.replaymod.core.versions.MCVer.getMinecraft;
import static org.joml.Math.PI_OVER_2_f;
import static org.joml.Math.PI_f;

@Mixin(Camera.class)
public abstract class Mixin_Omnidirectional_Rotation {
    @Shadow
    @Final
    //#if MC>=260200
    //$$ private static Vector3fc FORWARDS;
    //#else
    private static Vector3f FORWARDS;
    //#endif

    @Shadow
    @Final
    //#if MC>=260200
    //$$ private static Vector3fc UP;
    //#else
    private static Vector3f UP;
    //#endif

    @Shadow
    @Final
    //#if MC>=260200
    //$$ private static Vector3fc LEFT;
    //#else
    private static Vector3f LEFT;
    //#endif

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    private int matrixPropertiesDirty;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void replayModRender_setupCubicFrameRotation(CallbackInfo ci) {
        if (getHandler() == null || !(getHandler().data instanceof CubicOpenGlFrameCapturer.Data)) return;
        CubicOpenGlFrameCapturer.Data data = (CubicOpenGlFrameCapturer.Data) getHandler().data;

        switch (data) {
            case FRONT:
                break;
            case RIGHT:
                this.rotation.rotateY(-PI_OVER_2_f);
                break;
            case BACK:
                this.rotation.rotateY(PI_f);
                break;
            case LEFT:
                this.rotation.rotateY(PI_OVER_2_f);
                break;
            case TOP:
                this.rotation.rotateX(PI_OVER_2_f);
                break;
            case BOTTOM:
                this.rotation.rotateX(-PI_OVER_2_f);
                break;
        }

        // From setRotation
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }

    @Unique
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }
}
