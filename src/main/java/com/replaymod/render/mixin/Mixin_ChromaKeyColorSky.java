package com.replaymod.render.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Instead of rendering the normal sky, clears the screen with a uniform color for use with chroma keying.
 */
@Mixin(WorldRenderer.class)
public abstract class Mixin_ChromaKeyColorSky {
    @Shadow @Final private MinecraftClient client;

    //#if MC>=11800
    //$$ @Inject(
            //#if MC>=12102
            //$$ method = "method_62215",
            //#elseif MC>=12005
            //$$ method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            //#elseif MC>=11802
            //$$ method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            //#else
            //$$ method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V",
            //#endif
            //#if MC>=12104
            //$$ at = @At("HEAD"),
            //#elseif MC>=12102
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderPhase$Target;startDrawing()V", shift = At.Shift.AFTER),
            //#else
            //$$ at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", remap = false, shift = At.Shift.AFTER),
            //#endif
    //$$         cancellable = true)
    //#elseif MC>=11400 || 10710>=MC
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "renderSky(FI)V", at = @At("HEAD"), cancellable = true)
    //#endif
    private void chromaKeyingSky(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.client.gameRenderer).replayModRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                //#if MC>=12105
                //$$ RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                //$$         this.client.getFramebuffer().getColorAttachment(),
                //$$         (0xff << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue()
                //$$ );
                //#else
                GlStateManager.clearColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT
                        //#if MC>=11400 && MC<12102
                        , false
                        //#endif
                );
                //#endif
                ci.cancel();
            }
        }
    }
}
