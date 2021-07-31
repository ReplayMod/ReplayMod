//#if MC>=10800
package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

//#if MC>=11700
//$$ import net.minecraft.client.render.entity.EntityRendererFactory.Context;
//#else
import net.minecraft.client.render.entity.EntityRenderDispatcher;
//#endif

//#if MC>=11500
import net.minecraft.client.render.Frustum;
//#else
//$$ import net.minecraft.client.render.VisibleRegion;
//#endif

@Mixin(ArrowEntityRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    //#if MC>=11700
    //$$ protected MixinRenderArrow(Context context) {
    //$$     super(context);
    //$$ }
    //#else
    protected MixinRenderArrow(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }
    //#endif

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldRender(Entity entity,
                                //#if MC>=11500
                                Frustum camera,
                                //#else
                                //$$ VisibleRegion camera,
                                //#endif
                                double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
//#endif
