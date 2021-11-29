package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC>=11500
import net.minecraft.client.render.WorldRenderer;
//#else
//$$ import net.minecraft.client.render.GameRenderer;
//#endif

/**
 * Forces the sky to always render when chroma keying is active. Ordinarily it only renders when the render distance is
 * at 4 or greater.
 */
//#if MC>=11500
@Mixin(WorldRenderer.class)
//#else
//$$ @Mixin(GameRenderer.class)
//#endif
public abstract class Mixin_ChromaKeyForceSky {
    @Shadow @Final private MinecraftClient client;

    // FIXME preprocessor bug: should be able to remap these
    //#if MC>=11500
    @ModifyConstant(method = "render", constant = @Constant(intValue = 4))
    //#elseif MC>=11400
    //$$ @ModifyConstant(method = "renderCenter", constant = @Constant(intValue = 4))
    //#elseif MC>=10809
    //$$ @ModifyConstant(method = "updateCameraAndRender(FJ)V", constant = @Constant(intValue = 4))
    //#else
    //$$ @ModifyConstant(method = "updateCameraAndRender(F)V", constant = @Constant(intValue = 4))
    //#endif
    private int forceSkyWhenChromaKeying(int value) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.client.gameRenderer).replayModRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                return 0;
            }
        }
        return value;
    }
}
