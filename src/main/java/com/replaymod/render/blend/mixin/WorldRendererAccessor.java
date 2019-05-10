//#if MC>=10800
package com.replaymod.render.blend.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("chunkInfos")
    List getRenderInfos();
}
//#endif
