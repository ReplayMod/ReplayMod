package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectRenderer.class)
public abstract class MixinEffectRenderer {
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/Tesselator;FFFFFF)V"))
    private void renderNormalParticle(EntityFX fx, Tessellator tesselator, float partialTicks,
                                float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        renderParticle(fx, tesselator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }

    @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/Tesselator;FFFFFF)V"))
    private void renderLitParticle(EntityFX fx, Tessellator tesselator, float partialTicks,
                                 float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        renderParticle(fx, tesselator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }

    private void renderParticle(EntityFX fx, Tessellator tessellator, float partialTicks,
                                float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            Entity view = Minecraft.getMinecraft().renderViewEntity;
            // Align all particles towards the camera
            double dx = fx.prevPosX + (fx.posX - fx.prevPosX) * partialTicks - view.posX;
            double dy = fx.prevPosY + (fx.posY - fx.prevPosY) * partialTicks - view.posY;
            double dz = fx.prevPosZ + (fx.posZ - fx.prevPosZ) * partialTicks - view.posZ;
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);

            rotX = (float) Math.cos(yaw);
            rotZ = (float) Math.sin(yaw);
            rotXZ = (float) Math.cos(pitch);

            rotYZ = (float) (-rotZ * Math.sin(pitch));
            rotXY = (float) (rotX * Math.sin(pitch));
        }
        fx.renderParticle(tessellator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }
}
