//#if MC>=10904
package com.replaymod.render.blend.mixin;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemColorMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Accessor("colorMap")
    ItemColorMap getItemColors();
}
//#endif
