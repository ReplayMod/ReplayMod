package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11300
import net.minecraft.client.renderer.WorldRenderer;
//#else
//$$ import net.minecraft.client.renderer.RenderGlobal;
//#endif

//#if MC>=10904
import net.minecraft.util.math.BlockPos;
//#else
//#if MC>=10800
//$$ import net.minecraft.util.BlockPos;
//#endif
//#endif

//#if MC>=11300
@Mixin(WorldRenderer.class)
//#else
//$$ @Mixin(RenderGlobal.class)
//#endif
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

    //#if MC>=10800
    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "destroyBlockPartially", at = @At("HEAD"))
    //#endif
    public void saveBlockBreakProgressPacket(int breakerId,
                                             //#if MC>=10800
                                             BlockPos pos,
                                             //#else
                                             //$$ int x, int y, int z,
                                             //#endif
                                             int progress, CallbackInfo info) {
        if (recordingEventHandler != null) {
            recordingEventHandler.onBlockBreakAnim(breakerId,
                    //#if MC>=10800
                    pos,
                    //#else
                    //$$ x, y, z,
                    //#endif
                    progress);
        }
    }
}
