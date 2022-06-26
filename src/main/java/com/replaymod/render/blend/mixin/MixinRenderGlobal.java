//#if MC>=10800 && MC<11900
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import com.replaymod.render.blend.exporters.TileEntityExporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import net.minecraft.client.render.Frustum;
//#else
//$$ import net.minecraft.client.render.VisibleRegion;
//#endif

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#else
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC>=11400
import net.minecraft.client.render.WorldRenderer;
//#else
//$$ import net.minecraft.client.renderer.RenderGlobal;
//#endif

//#if MC>=11400
@Mixin(WorldRenderer.class)
//#else
//$$ @Mixin(RenderGlobal.class)
//#endif
public abstract class MixinRenderGlobal {

    // FIXME wither skull ._. mojang pls

    //#if MC>=11500
    @Inject(method = "renderEntity", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "renderEntities",
    //$$         at = @At(value = "INVOKE",
                    //#if MC>=10904
                    //$$ target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;FZ)V"))
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntitySimple(Lnet/minecraft/entity/Entity;F)Z"))
                    //#endif
    //#endif
    private void preEntityRender(CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).preEntitiesRender();
        }
    }

    //#if MC>=11500
    @Inject(method = "renderEntity", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "renderEntities",
    //$$         at = @At(value = "INVOKE",
                    //#if MC>=10904
                    //$$ target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;FZ)V",
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntitySimple(Lnet/minecraft/entity/Entity;F)Z",
                    //#endif
    //$$                 shift = At.Shift.AFTER))
    //#endif
    private void postEntityRender(CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postEntitiesRender();
        }
    }

    //#if MC>=11500
    // FIXME
    //#else
    //$$ @Inject(method = "renderEntities", at = @At(
    //$$         value = "INVOKE",
            //#if MC>=11400
            //$$ target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FI)V"
            //#else
            //$$ target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntity(Lnet/minecraft/tileentity/TileEntity;FI)V"
            //#endif
    //$$ ))
    //$$ private void preTileEntityRender(CallbackInfo ci) {
    //$$     BlendState blendState = BlendState.getState();
    //$$     if (blendState != null) {
    //$$         blendState.get(TileEntityExporter.class).preTileEntitiesRender();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "renderEntities", at = @At(
    //$$         value = "INVOKE",
            //#if MC>=11400
            //$$ target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FI)V",
            //#else
            //$$ target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntity(Lnet/minecraft/tileentity/TileEntity;FI)V",
            //#endif
    //$$         shift = At.Shift.AFTER
    //$$ ))
    //$$ private void postTileEntityRender(CallbackInfo ci) {
    //$$     BlendState blendState = BlendState.getState();
    //$$     if (blendState != null) {
    //$$         blendState.get(TileEntityExporter.class).postTileEntitiesRender();
    //$$     }
    //$$ }
    //#endif
}
//#endif
