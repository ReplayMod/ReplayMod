package com.replaymod.extras.playeroverview.mixin;

import com.replaymod.extras.ReplayModExtras;
import com.replaymod.extras.playeroverview.PlayerOverview;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
 */
@Mixin(value = Render.class, priority = 1200)
public abstract class MixinRender {
    @Inject(method = "shouldRender", at=@At("HEAD"), cancellable = true)
    public void replayModExtras_isPlayerHidden(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> ci) {
        ReplayModExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (playerOverview.isHidden(player.getUniqueID())) {
                    ci.setReturnValue(false);
                }
            }
        });
    }
}
