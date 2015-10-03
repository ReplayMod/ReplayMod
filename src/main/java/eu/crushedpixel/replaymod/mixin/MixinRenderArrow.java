package eu.crushedpixel.replaymod.mixin;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderArrow.class)
public abstract class MixinRenderArrow extends Render {
    protected MixinRenderArrow(RenderManager renderManager) {
        super(renderManager);
    }

    // TODO
//    @Override
//    public boolean shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ) {
//        return ReplayHandler.isInReplay() || super.shouldRender(entity, camera, camX, camY, camZ);
//    }
}
