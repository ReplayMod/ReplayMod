package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderArrow.class)
public abstract class MixinRenderArrow extends Render {
    protected MixinRenderArrow(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public boolean shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ) {
        return ReplayHandler.isInReplay() || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
