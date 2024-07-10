package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = SystemTimeUniforms.Timer.class, remap = false)
public class Mixin_FakeSystemTimeUniforms_Iris {
    @ModifyVariable(method = "beginFrame", at = @At("HEAD"), argsOnly = true)
    private long useReplayTimeDuringRender(long frameStartTimeNs) {
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        EntityRendererHandler entityRendererHandler =
            ((EntityRendererHandler.IEntityRenderer) gameRenderer).replayModRender_getHandler();
        if (entityRendererHandler != null) {
            frameStartTimeNs = entityRendererHandler.getFakeFinishTimeNano();
        }
        return frameStartTimeNs;
    }
}
