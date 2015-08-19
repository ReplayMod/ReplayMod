package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.video.EntityRendererHandler;
import eu.crushedpixel.replaymod.video.capturer.CubicOpenGlFrameCapturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Inject(method = "cacheActiveRenderInfo", at = @At("RETURN"))
    public void fixHeadRotationForAnimals(World world, FontRenderer font, Entity view, Entity target, GameSettings settings, float partialRenderTick, CallbackInfo ci) {
        if (view instanceof EntityAnimal && !((EntityAnimal) view).isPlayerSleeping()) {
            EntityAnimal e = (EntityAnimal) view;
            this.playerViewY = e.prevRotationYawHead + (e.rotationYawHead - e.prevRotationYawHead) * partialRenderTick;
        }
    }
}
