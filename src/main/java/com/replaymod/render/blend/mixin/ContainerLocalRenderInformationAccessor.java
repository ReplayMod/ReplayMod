//#if MC>=10800
package com.replaymod.render.blend.mixin;

import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=11300
@Mixin(targets = "net/minecraft/client/renderer/WorldRenderer$ContainerLocalRenderInformation")
//#else
//$$ @Mixin(targets = "net/minecraft/client/renderer/RenderGlobal$ContainerLocalRenderInformation")
//#endif
public interface ContainerLocalRenderInformationAccessor {
    @Accessor
    RenderChunk getRenderChunk();
}
//#endif
