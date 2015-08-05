package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import net.minecraft.client.renderer.entity.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderItem.class)
public class MixinRenderItem {
    @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    private long getEnchantmentTime() {
        return EnchantmentTimer.getEnchantmentTime();
    }
}
