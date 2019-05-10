//#if MC>=10800
package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ArrowEntityRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    protected MixinRenderArrow(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isVisible(Entity entity, VisibleRegion camera, double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.isVisible(entity, camera, camX, camY, camZ);
    }
}
//#endif
