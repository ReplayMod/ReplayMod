package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements RecordingEventHandler.RecordingEventSender {

    private RecordingEventHandler recordingEventHandler;

    @Override
    public void setRecordingEventHandler(RecordingEventHandler recordingEventHandler) {
        this.recordingEventHandler = recordingEventHandler;
    }

    @Override
    public RecordingEventHandler getRecordingEventHandler() {
        return recordingEventHandler;
    }

    @Inject(method = "destroyBlockPartially", at = @At("HEAD"))
    public void saveBlockBreakProgressPacket(int breakerId, int x, int y, int z, int progress, CallbackInfo info) {
        if (recordingEventHandler != null) {
            recordingEventHandler.onBlockBreakAnim(breakerId, x, y, z, progress);
        }
    }
}
