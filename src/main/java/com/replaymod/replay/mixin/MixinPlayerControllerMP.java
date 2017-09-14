package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {

    @Shadow
    private Minecraft mc;

    @Shadow
    private NetHandlerPlayClient netClientHandler;

    @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityClientPlayerMP> ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(mc, worldIn, mc.getSession(), netClientHandler, statFileWriter));
            ci.cancel();
        }
    }

    // Prevent the disconnect GUI from being opened during the short time when the replay is restarted
    // at which the old network manager is closed but still getting ticked (hence the disconnect GUI opening).
    @Inject(method = "updateController", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_onlyTickNeverDisconnect(CallbackInfo ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            if (netClientHandler.getNetworkManager().isChannelOpen()) {
                netClientHandler.getNetworkManager().processReceivedPackets();
            }
            ci.cancel();
        }
    }
}
