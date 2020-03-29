// FIXME 1.15
//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.TileEntityExporter;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {

    //#if MC>=11400
    //#if MC>=11400
    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;DDDFIZ)V",
            at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIZ)V",
    //$$         at = @At("HEAD"))
    //#endif
    public void preRender(BlockEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, boolean hasNoBlock, CallbackInfo ci) {
        float alpha = 1;
    //#else
    //#if MC>=11200
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
    //$$         at = @At("HEAD"))
    //$$ public void preRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, float alpha, CallbackInfo ci) {
    //#else
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFI)V",
    //$$         at = @At("HEAD"))
    //$$ public void preRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, CallbackInfo ci) {
    //$$    float alpha = 1;
    //#endif
    //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(TileEntityExporter.class).preRender(tileEntity, x, y, z, renderPartialTicks, destroyStage, alpha);
        }
    }

    //#if MC>=11400
    //#if MC>=11400
    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;DDDFIZ)V",
            at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIZ)V",
    //$$         at = @At("RETURN"))
    //#endif
    public void postRender(BlockEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, boolean hasNoBlock, CallbackInfo ci) {
    //#else
    //#if MC>=11200
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
    //$$         at = @At("RETURN"))
    //$$ public void postRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, float alpha, CallbackInfo ci) {
    //#else
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFI)V",
    //$$         at = @At("RETURN"))
    //$$ public void postRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, CallbackInfo ci) {
    //#endif
    //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(TileEntityExporter.class).postRender();
        }
    }
}
//#endif
