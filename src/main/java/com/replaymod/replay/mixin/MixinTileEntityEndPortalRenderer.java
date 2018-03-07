package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=10800
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
//#else
//$$ import net.minecraft.client.renderer.tileentity.RenderEndPortal;
//#endif

//#if MC>=10800
@Mixin(TileEntityEndPortalRenderer.class)
//#else
//$$ @Mixin(RenderEndPortal.class)
//#endif
public class MixinTileEntityEndPortalRenderer {
    //#if MC>=10809
    @Redirect(method = "renderTileEntityAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //#if MC>=10800
    //$$ @Redirect(method = "func_180544_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#endif
    //#endif
    private long replayModReplay_getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return Minecraft.getSystemTime();
    }
}
