package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11400
import net.minecraft.util.SystemUtil;
//#else
//$$ import net.minecraft.client.Minecraft;
//#endif

@Mixin(EndPortalBlockEntityRenderer.class)
public class MixinTileEntityEndPortalRenderer {
    //#if MC>=11400
    @Redirect(method = "method_3591", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SystemUtil;getMeasuringTimeMs()J"))
    //#else
    //#if MC>=11200
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //#if MC>=10809
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //#if MC>=10800
    //$$ @Redirect(method = "func_180544_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#else
    //$$ @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#endif
    //#endif
    //#endif
    //#endif
    private long replayModReplay_getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        //#if MC>=11400
        return SystemUtil.getMeasuringTimeMs();
        //#else
        //$$ return Minecraft.getSystemTime();
        //#endif
    }
}
