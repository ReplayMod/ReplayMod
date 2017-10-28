package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
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
    private NetHandlerPlayClient connection;

    @Inject(method = "func_192830_a", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, RecipeBook recipeBook, CallbackInfoReturnable<EntityPlayerSP> ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(mc, worldIn, connection, statisticsManager, recipeBook));
            ci.cancel();
        }
    }

    @Inject(method = "isSpectator", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_isSpectator(CallbackInfoReturnable<Boolean> ci) {
        if (mc.player instanceof CameraEntity) { // this check should in theory not be required
            ci.setReturnValue(mc.player.isSpectator());
        }
    }
}
