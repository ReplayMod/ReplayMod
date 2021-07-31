// Appears to no longer be needed in 1.14+ (maybe not even 1.13?)
//#if MC>=10800
package com.replaymod.replay.mixin;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Shadow
    public float playerViewY;

    @Inject(method = "cacheActiveRenderInfo", at = @At("RETURN"))
    public void fixHeadRotationForAnimals(World world, FontRenderer font, Entity view, Entity target, GameSettings settings, float partialRenderTick, CallbackInfo ci) {
        if (view instanceof EntityAnimal && !((EntityAnimal) view).isPlayerSleeping()) {
            EntityAnimal e = (EntityAnimal) view;
            this.playerViewY = e.prevRotationYawHead + (e.rotationYawHead - e.prevRotationYawHead) * partialRenderTick;
        }
    }
}
//#endif
