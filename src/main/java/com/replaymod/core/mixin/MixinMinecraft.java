package com.replaymod.core.mixin;

import com.replaymod.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//#if MC>=11300
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$ import com.replaymod.replay.InputReplayTimer;
//$$ import org.lwjgl.input.Mouse;
//#endif

import java.io.IOException;

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

    //#if MC>=11300
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
    //#else
    //#if MC>=10904
    //$$ @Shadow protected abstract void runTickKeyboard() throws IOException;
    //$$ @Shadow protected abstract void runTickMouse() throws IOException;
    //$$
    //$$ @Override
    //$$ public void replayModRunTickKeyboard() {
    //$$     try {
    //$$         runTickKeyboard();
    //$$     } catch (IOException e) {
    //$$         e.printStackTrace();
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public void replayModRunTickMouse() {
    //$$     try {
    //$$         runTickMouse();
    //$$     } catch (IOException e) {
    //$$         e.printStackTrace();
    //$$     }
    //$$ }
    //#endif
    //$$ @Redirect(
            //#if MC>=10904
            //$$ method = "runTickMouse",
            //#else
            //$$ method = "runTick",
            //#endif
    //$$         at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false)
    //$$ )
    //$$ private static int scroll() {
    //$$     int wheel = Mouse.getEventDWheel();
    //$$     InputReplayTimer.handleScroll(wheel);
    //$$     return wheel;
    //$$ }
    //#endif
}
