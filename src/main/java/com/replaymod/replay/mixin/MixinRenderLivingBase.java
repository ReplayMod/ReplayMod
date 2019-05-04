package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=10904
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#else
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#endif

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=10904
@Mixin(RenderLivingBase.class)
//#else
//$$ @Mixin(RendererLivingEntity.class)
//#endif
public abstract class MixinRenderLivingBase {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_canRenderInvisibleName(EntityLivingBase entity, CallbackInfoReturnable<Boolean> ci) {
        EntityPlayer thePlayer = getMinecraft().player;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }

    @Redirect(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11400
                    //$$ target = "Lnet/minecraft/entity/LivingEntity;canSeePlayer(Lnet/minecraft/entity/player/PlayerEntity;)Z"
                    //#else
                    target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"
                    //#endif
            )
    )
    private boolean replayModReplay_shouldInvisibleNotBeRendered(EntityLivingBase entity, EntityPlayer thePlayer) {
        return thePlayer instanceof CameraEntity || entity.isInvisibleToPlayer(thePlayer);
    }
}
