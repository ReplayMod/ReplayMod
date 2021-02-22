package com.replaymod.recording.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.handler.RecordingEventHandler.RecordingEventSender;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public abstract class MixinNetHandlerLoginClient {

    @Final @Shadow
    private ClientConnection connection;

    @Inject(method = "onQueryRequest", at=@At("HEAD"))
    private void earlyInitiateRecording(LoginQueryRequestS2CPacket packet, CallbackInfo ci) {
        initiateRecording(packet);
    }

    @Inject(method = "onLoginSuccess", at=@At("HEAD"))
    private void lateInitiateRecording(LoginSuccessS2CPacket packet, CallbackInfo ci) {
        initiateRecording(packet);
    }

    private void initiateRecording(Packet<?> packet) {
        RecordingEventSender eventSender = (RecordingEventSender) MCVer.getMinecraft().worldRenderer;
        if (eventSender.getRecordingEventHandler() != null) {
            return; // already recording
        }
        ReplayModRecording.instance.initiateRecording(this.connection);
        if (eventSender.getRecordingEventHandler() != null) {
            eventSender.getRecordingEventHandler().onPacket(packet);
        }
    }
}
