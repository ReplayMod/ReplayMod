package com.replaymod.extras.playeroverview.mixin;

import com.replaymod.extras.ReplayModExtras;
import com.replaymod.extras.playeroverview.PlayerOverview;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * This mixin prevents players that are hidden in the PlayerOverview from being rendered.
 *
 * Cancelling the RenderPlayerEvent.Pre is insufficient because it affects neither the shadows nor the fire texture.
 * See: https://github.com/MinecraftForge/MinecraftForge/issues/2987
 *
 * The previous solution was to overwrite the RenderPlayer instances which has been dropped in favor of this one
 * because it is less compatible with other mods whereas this one should be fine as long as no other mod completely
 * overwrites the methods we modify in this mixin.
 * One example of the previous solution breaking is when used with VanillaEnhancements because it replaces the
 * RenderManager with a new custom one which in turn will reset our registered RenderPlayer instances because
 * it does so after we have already registered with the old RenderManager.
 */
@Mixin(value = RenderPlayer.class, priority = 1200)
public abstract class MixinRenderPlayer extends RendererLivingEntity {
    public MixinRenderPlayer(ModelBase p_i1261_1_, float p_i1261_2_) {
        super(p_i1261_1_, p_i1261_2_);
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double d1, double d2, double d3, float f1, float f2) {
        UUID uuid = entity.getUniqueID();
        if (!ReplayModExtras.instance.get(PlayerOverview.class).map(po -> po.isHidden(uuid)).orElse(false)) {
            super.doRenderShadowAndFire(entity, d1, d2, d3, f1, f2);
        }
    }
}
