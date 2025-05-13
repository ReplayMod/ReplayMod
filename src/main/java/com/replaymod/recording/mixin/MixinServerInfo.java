package com.replaymod.recording.mixin;

import com.replaymod.recording.ServerInfoExt;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerInfo.class)
public abstract class MixinServerInfo implements ServerInfoExt {
    private Boolean autoRecording;

    @Override
    public Boolean getAutoRecording() {
        return autoRecording;
    }

    @Override
    public void setAutoRecording(Boolean autoRecording) {
        this.autoRecording = autoRecording;
    }

    @Inject(method = "serialize", at = @At("RETURN"))
    private void serialize(CallbackInfoReturnable<CompoundTag> ci) {
        CompoundTag tag = ci.getReturnValue();
        if (autoRecording != null) {
            tag.putBoolean("autoRecording", autoRecording);
        }
    }

    @Inject(method = "deserialize", at = @At("RETURN"))
    private static void deserialize(CompoundTag tag, CallbackInfoReturnable<ServerInfo> ci) {
        ServerInfoExt serverInfo = ServerInfoExt.from(ci.getReturnValue());
        if (tag.contains("autoRecording")) {
            //#if MC>=12105
            //$$ serverInfo.setAutoRecording(tag.getBoolean("autoRecording").orElseThrow());
            //#else
            serverInfo.setAutoRecording(tag.getBoolean("autoRecording"));
            //#endif
        }
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void copyFrom(ServerInfo serverInfo, CallbackInfo ci) {
        ServerInfoExt from = ServerInfoExt.from(serverInfo);
        this.autoRecording = from.getAutoRecording();
    }
}
