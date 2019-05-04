//#if MC>=11300
package com.replaymod.replay.mixin;

import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardListener.class)
public abstract class MixinKeyboardListener {
    @Redirect(
            method = "onKeyEvent",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11400
                    //$$ target = "Lnet/minecraft/client/util/ScreenshotUtils;method_1662(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/gl/GlFramebuffer;Ljava/util/function/Consumer;)V"
                    //#else
                    target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;Ljava/util/function/Consumer;)V"
                    //#endif
            )
    )
    private void takeScreenshot(
            File p_148260_0_,
            //#if MC>=11400
            //$$ String something,
            //#endif
            int p_148260_1_,
            int p_148260_2_,
            Framebuffer p_148260_3_,
            Consumer<ITextComponent> p_148260_4_
    ) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            AdvancedScreenshots.take();
        } else {
            ScreenShotHelper.saveScreenshot(
                    p_148260_0_,
                    //#if MC>=11400
                    //$$ something,
                    //#endif
                    p_148260_1_,
                    p_148260_2_,
                    p_148260_3_,
                    p_148260_4_
            );
        }
    }
}
//#endif
