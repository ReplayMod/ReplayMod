//#if MC>=10800
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ItemExporter;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11400
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
//#else
//#if MC>=10904
//$$ import net.minecraft.client.renderer.RenderItem;
//$$ import net.minecraft.client.renderer.block.model.IBakedModel;
//#else
//$$ import net.minecraft.client.renderer.entity.RenderItem;
//$$ import net.minecraft.client.resources.model.IBakedModel;
//#endif
//#endif

//#if MC>=11400
@Mixin(ItemRenderer.class)
//#else
//$$ @Mixin(RenderItem.class)
//#endif
public abstract class MixinRenderItem {
    //#if MC>=11500
    @Inject(method = "renderBakedItemModel", at = @At("HEAD"))
    private void onRenderModel(BakedModel model, ItemStack stack, int int_1, int int_2, MatrixStack matrixStack_1, VertexConsumer vertexConsumer_1, CallbackInfo ci) {
    //#else
    //#if MC>=11400
    //$$ @Inject(method = "renderItemModel", at = @At("HEAD"))
    //#else
    //#if MC>=11400
    //$$ @Inject(method = "renderModel(Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V",
    //$$         at = @At("HEAD"))
    //#else
    //#if MC>=10904
    //$$ @Inject(method = "renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V",
    //$$         at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "renderModel(Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V",
    //$$         at = @At("HEAD"))
    //#endif
    //#endif
    //#endif
    //$$ private void onRenderModel(BakedModel model, ItemStack stack, CallbackInfo ci) {
    //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ItemExporter.class).onRender(this, model, stack);
        }
    }
}
//#endif
