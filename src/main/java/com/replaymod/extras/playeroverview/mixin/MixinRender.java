package com.replaymod.extras.playeroverview.mixin;

import com.replaymod.extras.ReplayModExtras;
import com.replaymod.extras.playeroverview.PlayerOverview;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//#if MC>=10800
import net.minecraft.client.render.VisibleRegion;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

/**
 * This mixin prevents players that are hidden in the PlayerOverview from being rendered.
 *
 * Cancelling the RenderPlayerEvent.Pre is insufficient because it affects neither the shadows nor the fire texture.
 * See: https://github.com/MinecraftForge/MinecraftForge/issues/2987
 *
 * The previous solution was to overwrite the RenderPlayer instances which has been dropped in favor of this one
 * because it is less compatible with other mods whereas this one should be fine as long as no other mod completely
 * overwrites the shouldRender method.
 * One example of the previous solution breaking is when used with VanillaEnhancements because it replaces the
 * RenderManager with a new custom one which in turn will reset our registered RenderPlayer instances because
 * it does so after we have already registered with the old RenderManager.
 *
 * For 1.7.10, that method doesn't exist, so we use a combination of the event and inject into
 */
@Mixin(value = EntityRenderer.class, priority = 1200)
public abstract class MixinRender {
    //#if MC>=10800
    @Inject(method = "isVisible", at=@At("HEAD"), cancellable = true)
    public void replayModExtras_isPlayerHidden(Entity entity, VisibleRegion camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> ci) {
        ReplayModExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                if (playerOverview.isHidden(player.getUuid())) {
                    ci.setReturnValue(false);
                }
            }
        });
    }
    //#else
    //$$ @Inject(method = "doRenderShadowAndFire", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModExtras_isPlayerHidden(Entity entity, double x, double y, double z, float yaw, float time, CallbackInfo ci) {
    //$$     ReplayModExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
    //$$         if (entity instanceof EntityPlayer) {
    //$$             EntityPlayer player = (EntityPlayer) entity;
    //$$             if (playerOverview.isHidden(player.getUniqueID())) {
    //$$                 ci.cancel();
    //$$             }
    //$$         }
    //$$     });
    //$$ }
    //#endif
}
