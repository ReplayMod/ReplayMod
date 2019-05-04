//#if MC>=11300
package com.replaymod.core.mixin;

import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.versions.MCVer;
import de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import net.minecraft.util.NonBlockingThreadExecutor;
//#endif

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
        //#if MC>=11400
        //$$ extends NonBlockingThreadExecutor<Runnable>
        //#endif
        implements MCVer.MinecraftMethodAccessor {
    //#if MC>=11400
    //$$ public MixinMinecraft(String string_1) { super(string_1); }
    //#endif

    @Shadow protected abstract void processKeyBinds();

    @Override
    public void replayModProcessKeyBinds() {
        processKeyBinds();
    }

    //#if MC>=11400
    //$$ @Override
    //$$ public void replayModExecuteTaskQueue() {
    //$$     executeTaskQueue();
    //$$ }
    //#endif

    @Inject(method = "runGameLoop",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V"))
    private void preRender(boolean unused, CallbackInfo ci) {
        PreRenderCallback.EVENT.invoker().preRender();
    }

    @Inject(method = "runGameLoop",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V",
                    shift = At.Shift.AFTER))
    private void postRender(boolean unused, CallbackInfo ci) {
        PostRenderCallback.EVENT.invoker().postRender();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        PreTickCallback.EVENT.invoker().preTick();
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;"))
    private void openGuiScreen(GuiScreen newGuiScreen, CallbackInfo ci) {
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(newGuiScreen);
    }
}
//#endif
