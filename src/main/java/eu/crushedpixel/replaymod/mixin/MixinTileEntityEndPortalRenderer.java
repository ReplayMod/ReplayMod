package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntityEndPortalRenderer.class)
public class MixinTileEntityEndPortalRenderer {
    @Redirect(method = "func_180544_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    private long getEnchantmentTime() {
        return EnchantmentTimer.getEnchantmentTime();
    }
}
