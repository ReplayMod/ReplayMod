package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.TileEntityExporter;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {

    //#if MC>=11200
    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
            at = @At("HEAD"))
    public void preRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, float alpha, CallbackInfo ci) {
    //#else
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFI)V",
    //$$         at = @At("HEAD"))
    //$$ public void preRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, CallbackInfo ci) {
    //$$    float alpha = 1;
    //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(TileEntityExporter.class).preRender(tileEntity, x, y, z, renderPartialTicks, destroyStage, alpha);
        }
    }

    //#if MC>=11200
    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
            at = @At("RETURN"))
    public void postRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, float alpha, CallbackInfo ci) {
    //#else
    //$$ @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDFI)V",
    //$$         at = @At("RETURN"))
    //$$ public void postRender(TileEntity tileEntity, double x, double y, double z, float renderPartialTicks, int destroyStage, CallbackInfo ci) {
    //#endif
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(TileEntityExporter.class).postRender();
        }
    }
}
