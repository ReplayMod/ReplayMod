package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.registry.PlayerHandler;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;

public class InvisibilityRender extends RenderPlayer {

    public InvisibilityRender(RenderManager renderManager) {
        super(renderManager);
    }

    public InvisibilityRender(RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, useSmallArms);
    }

    @Override
    public boolean shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ) {
        if(PlayerHandler.isHidden(entity.getEntityId())) return false;
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
