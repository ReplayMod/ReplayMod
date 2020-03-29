package com.replaymod.render.mixin;

import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
//$$ import net.minecraft.client.util.math.MatrixStack;
//$$ import net.minecraft.client.util.math.Vector3f;
//#else
import org.lwjgl.opengl.GL11;
//#endif

import static com.replaymod.core.versions.MCVer.getMinecraft;

//#if MC>=11500
//$$ @Mixin(value = net.minecraft.client.render.GameRenderer.class)
//#else
//#if MC>=11400
@Mixin(value = net.minecraft.client.render.Camera.class)
//#else
//$$ @Mixin(value = net.minecraft.client.renderer.EntityRenderer.class)
//#endif
//#endif
public abstract class Mixin_CubicRotation {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    //#if MC>=11500
    //$$ @Inject(method = "renderWorld", at = @At("HEAD"))
    //#else
    //#if MC>=11400
    @Inject(method = "update", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "orientCamera", at = @At("HEAD"))
    //#endif
    //#endif
    private void replayModRender_setupCubicFrameRotation(
            //#if MC>=11500
            //$$ float partialTicks,
            //$$ long frameStartNano,
            //$$ MatrixStack matrixStack,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null && getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            CubicOpenGlFrameCapturer.Data data = (CubicOpenGlFrameCapturer.Data) getHandler().data;
            float angle = 0;
            float x = 0;
            float y = 0;
            switch (data) {
                case FRONT:
                    angle = 0;
                    x = 1;
                    break;
                case RIGHT:
                    angle = 90;
                    x = 1;
                    break;
                case BACK:
                    angle = 180;
                    x = 1;
                    break;
                case LEFT:
                    angle = -90;
                    x = 1;
                    break;
                case TOP:
                    angle = -90;
                    y = 1;
                    break;
                case BOTTOM:
                    angle = 90;
                    y = 1;
                    break;
            }
            //#if MC>=11500
            //$$ matrixStack.multiply(new Vector3f(x, y, 0).getDegreesQuaternion(angle));
            //#else
            GL11.glRotatef(angle, x, y, 0);
            //#endif
        }
        //#if MC<11500
        if (getHandler() != null && getHandler().omnidirectional) {
            // Minecraft goes back a little so we have to revert that
            //#if MC>=11400
            GL11.glTranslatef(0.0F, 0.0F, -0.05F);
            //#else
            //$$ GL11.glTranslatef(0.0F, 0.0F, 0.1F);
            //#endif
        }
        //#endif
    }
}
