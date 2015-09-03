package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.video.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void areAllNamesHidden(EntityLivingBase entity, CallbackInfoReturnable<Boolean> ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getMinecraft().entityRenderer).getHandler();
        if (handler != null && handler.getOptions().isHideNameTags()) {
            ci.setReturnValue(false);
            ci.cancel();
        }
    }

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean shouldInvisibleNotBeRendered(EntityLivingBase entity, EntityPlayer thePlayer) {
        if(ReplaySettings.ReplayOptions.renderInvisible.getValue() == Boolean.TRUE|| !ReplayHandler.isInReplay()) {
            return entity.isInvisibleToPlayer(thePlayer);
        }
        return true; //the original method inverts the return value
    }
}
