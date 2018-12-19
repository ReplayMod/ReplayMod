package com.replaymod.recording.mixin;

import com.replaymod.recording.ReplayModRecording;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10800
// FIXME event not (yet?) in 1.13 import net.minecraftforge.fml.common.network.FMLNetworkEvent;
//#else
//$$ import cpw.mods.fml.common.network.FMLNetworkEvent;
//#endif

@Mixin(NetHandlerLoginClient.class)
public abstract class MixinNetHandlerLoginClient {

    @Shadow
    //#if MC>=10800
    private NetworkManager networkManager;
    //#else
    //$$ private NetworkManager field_147393_d;
    //#endif

    //#if MC>=11300
    @Inject(method = "func_209521_a", at=@At("HEAD"))
    //#else
    //$$ /**
    //$$  * Starts the recording right before switching into PLAY state.
    //$$  * We cannot use the {@link FMLNetworkEvent.ClientConnectedToServerEvent}
    //$$  * as it only fires after the forge handshake.
    //$$  */
    //$$ @Inject(method = "handleLoginSuccess", at=@At("HEAD"))
    //#endif
    public void replayModRecording_initiateRecording(CallbackInfo cb) {
        //#if MC>=10800
        ReplayModRecording.instance.initiateRecording(networkManager);
        //#else
        //$$ ReplayModRecording.instance.initiateRecording(field_147393_d);
        //#endif
    }

    //#if MC>=11200
    @Inject(method = "handleLoginSuccess", at=@At("RETURN"))
    public void replayModRecording_raceConditionWorkAround(CallbackInfo cb) {
        networkManager.channel().config().setAutoRead(true);
    }
    //#endif
}
