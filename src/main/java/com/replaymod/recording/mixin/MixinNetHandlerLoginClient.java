package com.replaymod.recording.mixin;

import com.replaymod.recording.ReplayModRecording;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerLoginClient.class)
public abstract class MixinNetHandlerLoginClient {

    @Shadow
    private @Final NetworkManager field_147393_d;

    /**
     * Starts the recording right before switching into PLAY state.
     * We cannot use the {@link FMLNetworkEvent.ClientConnectedToServerEvent}
     * as it only fires after the forge handshake.
     */
    @Inject(method = "handleLoginSuccess", at=@At("HEAD"))
    public void replayModRecording_initiateRecording(CallbackInfo cb) {
        ReplayModRecording.instance.initiateRecording(field_147393_d);
    }
}
