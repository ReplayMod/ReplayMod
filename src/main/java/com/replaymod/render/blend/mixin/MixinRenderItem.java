//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ItemExporter;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10904
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
//#else
//$$ import net.minecraft.client.renderer.entity.RenderItem;
//$$ import net.minecraft.client.resources.model.IBakedModel;
//#endif

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {
    //#if MC>=10904
    @Inject(method = "renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "renderModel(Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V",
    //$$         at = @At("HEAD"))
    //#endif
    private void onRenderModel(IBakedModel model, ItemStack stack, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ItemExporter.class).onRender((RenderItem)(Object)this, model, stack);
        }
    }
}
//#endif
