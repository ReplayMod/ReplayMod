package com.replaymod.render.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public abstract class Mixin_Stereoscopic_HandRenderPass {
    // MC's builtin stereoscopic 3D feature was dropped
    // 1.12.2 and before
}
