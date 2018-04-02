package com.replaymod.render.mixin;

//#if MC<10904
//$$ import com.replaymod.render.hooks.EntityRendererHandler;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.particle.EffectRenderer;
//$$ import net.minecraft.client.particle.EntityFX;
//$$ import net.minecraft.entity.Entity;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$
//#if MC>=10800
//$$ import net.minecraft.client.renderer.WorldRenderer;
//#else
//$$ import net.minecraft.client.renderer.Tessellator;
//#endif
//$$
//$$ @Mixin(EffectRenderer.class)
//$$ public abstract class MixinEffectRenderer {
    //#if MC>=10800
    //#if MC>=10809
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;func_180434_a(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //#endif
    //$$ private void renderNormalParticle(EntityFX fx, WorldRenderer worldRenderer, Entity view, float partialTicks,
    //$$                                   float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(fx, worldRenderer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/Tessellator;FFFFFF)V"))
    //$$ private void renderNormalParticle(EntityFX fx, Tessellator tesselator, float partialTicks,
    //$$                                   float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(fx, tesselator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
//$$
    //#if MC>=10800
    //#if MC>=10809
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //#else
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;func_180434_a(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //#endif
    //$$ private void renderLitParticle(EntityFX fx, WorldRenderer worldRenderer, Entity view, float partialTicks,
    //$$                                float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(fx, worldRenderer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#else
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/Tessellator;FFFFFF)V"))
    //$$ private void renderLitParticle(EntityFX fx, Tessellator tessellator, float partialTicks,
    //$$                                float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(fx, tessellator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
//$$
//$$     private void renderParticle(EntityFX fx,
                                //#if MC>=10800
                                //$$ WorldRenderer worldRenderer, Entity view,
                                //#else
                                //$$ Tessellator tessellator,
                                //#endif
//$$                                 float partialTicks,
//$$                                 float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
//$$         EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).replayModRender_getHandler();
//$$         if (handler != null && handler.omnidirectional) {
            //#if MC<=10710
            //$$ Entity view = Minecraft.getMinecraft().renderViewEntity;
            //#endif
//$$             // Align all particles towards the camera
//$$             double dx = fx.prevPosX + (fx.posX - fx.prevPosX) * partialTicks - view.posX;
//$$             double dy = fx.prevPosY + (fx.posY - fx.prevPosY) * partialTicks - view.posY;
//$$             double dz = fx.prevPosZ + (fx.posZ - fx.prevPosZ) * partialTicks - view.posZ;
//$$             double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
//$$             double yaw = -Math.atan2(dx, dz);
//$$
//$$             rotX = (float) Math.cos(yaw);
//$$             rotZ = (float) Math.sin(yaw);
//$$             rotXZ = (float) Math.cos(pitch);
//$$
//$$             rotYZ = (float) (-rotZ * Math.sin(pitch));
//$$             rotXY = (float) (rotX * Math.sin(pitch));
//$$         }
        //#if MC>=10809
        //$$ fx.renderParticle(worldRenderer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
        //#else
        //#if MC>=10800
        //$$ fx.func_180434_a(worldRenderer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
        //#else
        //$$ fx.renderParticle(tessellator, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
        //#endif
        //#endif
//$$     }
//$$ }
//#endif
