package eu.crushedpixel.replaymod.mixin;

import com.replaymod.replay.camera.CameraEntity;
import eu.crushedpixel.replaymod.renderer.SpectatorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    public Minecraft mc;

    private SpectatorRenderer spectatorRenderer = new SpectatorRenderer();

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void renderSpectatorHand(float partialTicks, int renderPass, CallbackInfo ci) {
        // TODO Check that this gets cancelled in 360 and doesn't misbehave in SP/MP
        Entity currentEntity = Minecraft.getMinecraft().getRenderViewEntity();
        if (currentEntity instanceof EntityPlayer && !(currentEntity instanceof EntityPlayerSP)) {
            spectatorRenderer.renderSpectatorHand((EntityPlayer) currentEntity, partialTicks, renderPass);
        }
    }

    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", shift = At.Shift.AFTER, ordinal = 3))
    private void setupCameraRoll(float partialTicks, CallbackInfo ci) {
        if (mc.getRenderViewEntity() instanceof CameraEntity) {
            GL11.glRotated(((CameraEntity) mc.getRenderViewEntity()).roll, 0D, 0D, 1D);
        }
    }
}
