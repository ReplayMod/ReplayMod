package com.replaymod.render.mixin;

//#if MC>=10904
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ParticlesExporter;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11200
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.client.renderer.VertexBuffer;
//#endif

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {
    //#if MC>=11200
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void renderNormalParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
                                      float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }

    //#if MC>=11200
    @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void renderLitParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderLitParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
                                 float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }

    //#if MC>=11200
    private void renderParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#else
    //$$ private void renderParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
                                 float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            // Align all particles towards the camera
            double dx = particle.prevPosX + (particle.posX - particle.prevPosX) * partialTicks - view.posX;
            double dy = particle.prevPosY + (particle.posY - particle.prevPosY) * partialTicks - view.posY;
            double dz = particle.prevPosZ + (particle.posZ - particle.prevPosZ) * partialTicks - view.posZ;
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);

            rotX = (float) Math.cos(yaw);
            rotZ = (float) Math.sin(yaw);
            rotXZ = (float) Math.cos(pitch);

            rotYZ = (float) (-rotZ * Math.sin(pitch));
            rotXY = (float) (rotX * Math.sin(pitch));
        }
        BlendState blendState = BlendState.getState();
                if (blendState != null) {
                blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
        }
        particle.renderParticle(vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }
}
//#endif
