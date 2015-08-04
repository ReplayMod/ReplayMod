package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.video.EntityRendererHandler;
import eu.crushedpixel.replaymod.video.capturer.CubicOpenGlFrameCapturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Shadow
    private float playerViewX;

    @Shadow
    private float playerViewY;

    @Inject(method = "doRenderEntity", at = @At("HEAD"))
    private void reorientForCubicRendering(Entity entity, double dx, double dy, double dz, float iDoNotKnow, float partialTicks, boolean iDoNotCare, CallbackInfoReturnable ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).getHandler();
        if (handler != null && handler.data instanceof CubicOpenGlFrameCapturer.Data) {
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);
            playerViewX = (float) Math.toDegrees(pitch);
            playerViewY = (float) Math.toDegrees(yaw);
        }
    }
}
