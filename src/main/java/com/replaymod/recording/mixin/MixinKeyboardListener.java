//#if MC>=11300
package com.replaymod.recording.mixin;

import com.replaymod.core.mixin.KeyBindingAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardListener.class)
public abstract class MixinKeyboardListener {
    @Inject(method = "onKeyEvent", at = @At("HEAD"), cancellable = true)
    private void dispatchKeyEvent(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // FIXME this is a hack and will hopefully be done properly by forge before its release (MinecraftForge#5481)
        //       (and therefore before RM's release), this code will have to be removed then and handling code updated
        KeyBinding keyBindChat = MCVer.getMinecraft().gameSettings.keyBindChat;
        if (action == 1 && keyBindChat.matchesKey(key, scanCode)) {
            KeyBindingAccessor acc = (KeyBindingAccessor) keyBindChat;
            acc.setPressTime(acc.getPressTime() + 1);
            BasicEventHooks.fireKeyInput();
            ci.cancel();
        }
    }

    @Redirect(method = "onKeyEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;Ljava/util/function/Consumer;)V"))
    private void takeScreenshot(File p_148260_0_, int p_148260_1_, int p_148260_2_, Framebuffer p_148260_3_, Consumer<ITextComponent> p_148260_4_) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            AdvancedScreenshots.take();
        } else {
            ScreenShotHelper.saveScreenshot(p_148260_0_, p_148260_1_, p_148260_2_, p_148260_3_, p_148260_4_);
        }
    }
}
//#endif
