package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"))
    public void saveBlockBreakProgressPacket(int breakerId, BlockPos pos, int progress, CallbackInfo info) {
        if(ConnectionEventHandler.isRecording()) {
            EntityPlayer thePlayer = Minecraft.getMinecraft().thePlayer;
            if(thePlayer != null && breakerId == thePlayer.getEntityId()) {
                ConnectionEventHandler.insertPacket(new S25PacketBlockBreakAnim(breakerId, pos, progress));
            }
        }
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    private long getEnchantmentTime() {
        return EnchantmentTimer.getEnchantmentTime();
    }
}
