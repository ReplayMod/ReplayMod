package com.replaymod.render.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public abstract class Mixin_Omnidirectional_Camera {
    // Now handled by Camera.enablePanoramicMode
    // 1.21.11 and below
}
