package com.replaymod.core.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11400
//#if MC>=12100
//$$ import net.minecraft.client.render.RenderTickCounter;
//#endif

/**
 * MinecraftClient.tick() / render() caps replay tick processing at 10 ticks per render
 * frame. At replay speed 8x the tick length is 50/8 ≈ 6.25 ms, so any frame slower than
 * about 16 FPS hits that cap and the effective playback speed silently collapses back
 * toward 1x. We only lift the cap while a replay is active; off-replay behaviour is
 * unchanged because the redirect short-circuits to the original Math.min call.
 */
@Mixin(MinecraftClient.class)
public abstract class Mixin_LiftReplayTickRateCap {
    //#if MC>=12100
    //$$ @Redirect(
    //$$         method = "render",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(JZ)I",
    //$$                 ordinal = 0
    //$$         ),
    //$$         require = 1
    //$$ )
    //$$ private int replaymod$liftTickCap(RenderTickCounter.Dynamic counter, long timeMillis, boolean tick) {
    //$$     int ticks = counter.beginRenderTick(timeMillis, tick);
    //$$     if (replaymod$inReplay()) {
    //$$         return Math.min(200, ticks);
    //$$     }
    //$$     return Math.min(10, ticks);
    //$$ }
    //#else
    // The cap lives in MinecraftClient.render(boolean), called from runLoop. The first
    // Math.min(II)I invocation in that method is the per-frame tick count clamp.
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(II)I",
                    ordinal = 0
            ),
            require = 1
    )
    private int replaymod$liftTickCap(int cap, int ticks) {
        if (cap == 10 && replaymod$inReplay()) {
            // Allow up to 200 ticks/frame so that 8x replay speed @ 4 FPS still drains.
            // Beyond that, render-thread time dominates and a higher cap doesn't help.
            return Math.min(200, ticks);
        }
        return Math.min(cap, ticks);
    }
    //#endif

    private static boolean replaymod$inReplay() {
        ReplayModReplay mod = ReplayModReplay.instance;
        if (mod == null) return false;
        ReplayHandler handler = mod.getReplayHandler();
        return handler != null;
    }
}
//#else
//$$ // Pre-1.14 Minecraft did not expose the same ticks-per-frame cap pattern; leaving a
//$$ // no-op stub here keeps the mixin entry resolving on every preprocessor target.
//$$ @org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Minecraft.class)
//$$ public abstract class Mixin_LiftReplayTickRateCap {
//$$ }
//#endif
