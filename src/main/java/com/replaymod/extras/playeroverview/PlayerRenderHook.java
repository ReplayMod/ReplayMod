package com.replaymod.extras.playeroverview;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;

public class PlayerRenderHook extends RenderPlayer {
    private final PlayerOverview extra;

    public PlayerRenderHook(PlayerOverview extra, RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, useSmallArms);
        this.extra = extra;
    }

    @Override
    public boolean shouldRender(AbstractClientPlayer entity, ICamera camera, double camX, double camY, double camZ) {
        return !extra.isHidden(entity.getUniqueID()) && super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
