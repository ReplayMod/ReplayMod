package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP implements RecordingEventHandler.RecordingEventSender {

    @Shadow
    private Minecraft mc;

    // Redirects the call to playEvent without the initial player argument to the method with that argument
    // The new method will then play it and (if applicable) record it. (See MixinWorldClient)
    // This is necessary for the block break event (particles and sound) to be recorded. Otherwise it looks like the
    // event was emitted because of a packet (player will be null) and not as it actually was (by the player).
    @Redirect(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    public void replayModRecording_playEvent_fixed(World world, int type, BlockPos pos, int data) {
        world.playEvent(mc.player, type, pos, data);
    }
}
