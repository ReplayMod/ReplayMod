package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {

    @Shadow
    private Minecraft mc;

    @Shadow
    private NetHandlerPlayClient netClientHandler;

    @Inject(method = "func_178892_a", at=@At("HEAD"), cancellable = true)
    public void createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityPlayerSP> ci) {
        if (ReplayHandler.isInReplay()) {
            ci.setReturnValue(new CameraEntity(mc, worldIn, netClientHandler, statFileWriter));
            ci.cancel();
        }
    }
}
