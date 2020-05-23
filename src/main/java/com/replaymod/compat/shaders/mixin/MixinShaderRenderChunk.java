//#if MC>=10800
package com.replaymod.compat.shaders.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11500
import org.spongepowered.asm.mixin.Shadow;
//#endif

//#if MC>=11500
@Mixin(net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk.class)
//#else
//$$ @Mixin(net.minecraft.client.render.chunk.ChunkRenderer.class)
//#endif
public abstract class MixinShaderRenderChunk {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    //#if MC>=11500
    @Shadow private int rebuildFrame;

    @Shadow public abstract boolean shouldBuild();


    /**
     * So, for some reason OF though it'd be a good idea to drop the `shouldBuild` check from the chunk traversal in
     * the setupTerrain method (see the innermost if of the "iteration" profiler section).
     * Hint: It's not a good idea. It'll cause chunks to be queued for building which should not be built. And because
     *       they don't know any better, they'll even re-queue themselves (which looks like a race condition in vanilla
     *       MC) causing a live-lock when we try to force-build all chunks (or at least it would cause a lockup if it
     *       wasn't for the vanilla race condition which slowly causes some of the rebuild tasks to get lost over time,
     *       eventually, potentially all of them).
     * This is the most convenient place to re-introduce the check (setRebuildFrame would normally be called right after
     * shouldBuild, if it returns true).
     */
    @Inject(method = "setRebuildFrame", at = @At("HEAD"), cancellable = true)
    private void replayModCompat_OFHaveYouConsideredWhetherThisChunkShouldEvenBeBuilt(int rebuildFrame, CallbackInfoReturnable<Boolean> ci) {
        if (this.rebuildFrame == rebuildFrame) {
            // want to keep the fast path
            ci.setReturnValue(false);
        } else if (!this.shouldBuild()) {
            // this is the check which OF removed
            // So, I think I figured out the reason why optifine applies this change: It's so chunks which are outside
            // the server view distance are still rendered if they've previously compiled. The bug still stands but
            // fixing it breaks OF's broken feature, so to reduce the impact of our fix, we only apply it during
            // rendering where it affects us the most.
            if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null) return;
            ci.setReturnValue(false);
        }
    }
    //#endif

    /**
     *  Changes the RenderChunk#isPlayerUpdate method that Optifine adds
     *  to always return true while rendering so no chunks are being added
     *  to a separate rendering queue
     */
    @Inject(method = "isPlayerUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void replayModCompat_disableIsPlayerUpdate(CallbackInfoReturnable<Boolean> ci) {
        if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null) return;
        ci.setReturnValue(true);
    }


}
//#endif
