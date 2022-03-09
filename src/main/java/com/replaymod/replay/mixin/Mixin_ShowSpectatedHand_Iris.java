//#if MC>=11400
package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Pseudo
@Mixin(targets = "net.coderbot.iris.pipeline.HandRenderer", remap = false)
public abstract class Mixin_ShowSpectatedHand_Iris {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;getCurrentGameMode()Lnet/minecraft/world/GameMode;",
                    remap = true
            )
    )
    private GameMode getGameMode(ClientPlayerInteractionManager interactionManager) {
        ClientPlayerEntity camera = getMinecraft().player;
        if (camera instanceof CameraEntity) {
            // alternative doesn't really matter, the caller only checks for equality to SPECTATOR
            return camera.isSpectator() ? GameMode.SPECTATOR : GameMode.SURVIVAL;
        }
        return interactionManager.getCurrentGameMode();
    }
}
//#endif
