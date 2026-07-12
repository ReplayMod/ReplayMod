package com.replaymod.recording.mixin;

import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.handler.RecordingEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
//$$ import net.minecraft.client.multiplayer.ClientLevel;
//#else
import net.minecraft.client.render.WorldRenderer;
//#endif

//#if MC>=10800
import net.minecraft.util.math.BlockPos;
//#endif

//#if MC >= 26.2
//$$ @Mixin(ClientLevel.class)
//#else
@Mixin(WorldRenderer.class)
//#endif
public abstract class MixinRenderGlobal {
    //#if MC>=10800
    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"))
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
        RecordingEventHandler recordingEventHandler = ReplayModRecording.instance.getConnectionEventHandler().getRecordingEventHandler();
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
