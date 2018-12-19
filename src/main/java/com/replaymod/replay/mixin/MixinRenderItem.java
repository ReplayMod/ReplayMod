package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11300
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.Util;
//#else
//$$ import net.minecraft.client.Minecraft;
//#if MC>=10904
//$$ import net.minecraft.client.renderer.RenderItem;
//#else
//$$ import net.minecraft.client.renderer.entity.RenderItem;
//#endif
//#endif

//#if MC>=11300
@Mixin(ItemRenderer.class)
//#else
//$$ @Mixin(RenderItem.class)
//#endif
public class MixinRenderItem {
    //#if MC>=11300
    @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J"))
    //#else
    //$$ @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#endif
    private static long getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        //#if MC>=11300
        return Util.milliTime();
        //#else
        //$$ return Minecraft.getSystemTime();
        //#endif
    }
}
