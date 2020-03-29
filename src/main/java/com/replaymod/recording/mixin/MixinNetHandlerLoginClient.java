package com.replaymod.recording.mixin;

import com.replaymod.recording.ReplayModRecording;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.handler.RecordingEventHandler.RecordingEventSender;
import net.minecraft.network.Packet;
import net.minecraft.client.network.packet.LoginQueryRequestS2CPacket;
import net.minecraft.client.network.packet.LoginSuccessS2CPacket;
//#else
//$$ import net.minecraftforge.fml.common.network.FMLNetworkEvent;
//#endif

@Mixin(ClientLoginNetworkHandler.class)
public abstract class MixinNetHandlerLoginClient {

    @Shadow
    //#if MC>=10800
    private ClientConnection connection;
    //#else
    //$$ private NetworkManager field_147393_d;
    //#endif

    //#if MC>=11400
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
    //#else
    //$$ /**
    //$$  * Starts the recording right before switching into PLAY state.
    //$$  * We cannot use the {@link FMLNetworkEvent.ClientConnectedToServerEvent}
    //$$  * as it only fires after the forge handshake.
    //$$  */
    //$$ @Inject(method = "handleLoginSuccess", at=@At("HEAD"))
    //$$ public void replayModRecording_initiateRecording(CallbackInfo cb) {
        //#if MC>=10800
        //$$ ReplayModRecording.instance.initiateRecording(this.networkManager);
        //#else
        //$$ ReplayModRecording.instance.initiateRecording(this.field_147393_d);
        //#endif
    //$$ }
    //#endif

    // Race condition in Forge's networking (not sure if still present in 1.13)
    //#if MC>=11200 && MC<11400
    //$$ @Inject(method = "handleLoginSuccess", at=@At("RETURN"))
    //$$ public void replayModRecording_raceConditionWorkAround(CallbackInfo cb) {
    //$$     ((NetworkManagerAccessor) this.networkManager).getChannel().config().setAutoRead(true);
    //$$ }
    //#endif
}
