package eu.crushedpixel.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundHandler;setListener(Lnet/minecraft/entity/player/EntityPlayer;F)V"))
    public void setSoundSystemListener(SoundHandler soundHandler, EntityPlayer listener, float renderPartialTicks) {
        //TODO might no longer be necessary?
//        soundHandler.setListener(ReplayHandler.isInReplay() ? ReplayHandler.getCameraEntity() : listener, renderPartialTicks);
    }
}
