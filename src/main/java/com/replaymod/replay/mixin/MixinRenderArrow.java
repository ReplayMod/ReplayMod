//#if MC>=10800
package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

//#if MC>=11500
//$$ import net.minecraft.client.render.Frustum;
//#else
import net.minecraft.client.render.VisibleRegion;
//#endif

@Mixin(ArrowEntityRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    protected MixinRenderArrow(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isVisible(Entity entity,
                                //#if MC>=11500
                                //$$ Frustum camera,
                                //#else
                                VisibleRegion camera,
                                //#endif
                                double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.isVisible(entity, camera, camX, camY, camZ);
    }
}
//#endif
