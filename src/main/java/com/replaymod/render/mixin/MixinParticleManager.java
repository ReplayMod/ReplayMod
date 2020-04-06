package com.replaymod.render.mixin;

//#if MC>=10904
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ParticlesExporter;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11500
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Quaternion;
//#else
//$$ import com.replaymod.render.blend.mixin.ParticleAccessor;
//$$ import net.minecraft.client.render.BufferBuilder;
//#endif

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#else
//$$ import net.minecraft.entity.Entity;
//#endif

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {
    //#if MC>=11500
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"))
    private void buildOrientedGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            buildGeometry(particle, vertexConsumer, camera, partialTicks);
        } else {
            Quaternion rotation = camera.getRotation();
            Quaternion org = rotation.copy();
            try {
                Vec3d from = new Vec3d(0, 0, 1);
                Vec3d to = MCVer.getPosition(particle, partialTicks).subtract(camera.getPos()).normalize();
                Vec3d axis = from.crossProduct(to);
                rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dotProduct(to)));
                rotation.normalize();

                buildGeometry(particle, vertexConsumer, camera, partialTicks);
            } finally {
                rotation.set(org.getA(), org.getB(), org.getC(), org.getD());
            }
        }
    }

    private void buildGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
        }
        particle.buildGeometry(vertexConsumer, camera, partialTicks);
    }
    //#else
    //#if MC>=11200
    //#if MC>=11400
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/client/render/Camera;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, BufferBuilder vertexBuffer, Camera view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#endif
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
    //$$                                   float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //$$
    //$$ // Seems to be gone by 1.14
    //#if MC<11400
    //#if MC>=11200
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderLitParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderLitParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
    //$$                              float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
    //$$
    //$$ private void renderParticle(Particle particle,
    //$$                             BufferBuilder vertexBuffer,
                                //#if MC>=11400
                                //$$ Camera view,
                                //#else
                                //$$ Entity view,
                                //#endif
    //$$                             float partialTicks,
    //$$                             float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
    //$$     if (handler != null && handler.omnidirectional) {
    //$$         // Align all particles towards the camera
            //#if MC>=11400
            //$$ Vec3d pos = view.getPos();
            //#else
            //$$ Vec3d pos = new Vec3d(view.posX, view.posY, view.posZ);
            //#endif
    //$$         Vec3d d = MCVer.getPosition(particle, partialTicks).subtract(pos);
    //$$         double pitch = -Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
    //$$         double yaw = -Math.atan2(d.x, d.z);
    //$$
    //$$         rotX = (float) Math.cos(yaw);
    //$$         rotZ = (float) Math.sin(yaw);
    //$$         rotXZ = (float) Math.cos(pitch);
    //$$
    //$$         rotYZ = (float) (-rotZ * Math.sin(pitch));
    //$$         rotXY = (float) (rotX * Math.sin(pitch));
    //$$     }
    //$$     BlendState blendState = BlendState.getState();
    //$$             if (blendState != null) {
    //$$             blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
    //$$     }
    //$$     particle.buildGeometry(vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
}
//#endif
