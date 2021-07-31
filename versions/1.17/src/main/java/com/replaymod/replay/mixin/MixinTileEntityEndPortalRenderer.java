package com.replaymod.replay.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderSystem.class)
public class MixinTileEntityEndPortalRenderer {
    // All shaders now use the game time
}
