package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.registry.PlayerHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
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
        return !(PlayerHandler.isHidden(entity.getUniqueID()) || (ReplayHandler.isInReplay()
                && entity == Minecraft.getMinecraft().thePlayer))
                && super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
