package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11200
import net.minecraft.stats.RecipeBook;
//#endif
//#if MC>=10904
import net.minecraft.stats.StatisticsManager;
//#else
//$$ import net.minecraft.stats.StatFileWriter;
//#endif

//#if MC>=10800
import net.minecraft.client.entity.EntityPlayerSP;

import static com.replaymod.core.versions.MCVer.*;
//#else
//$$ import net.minecraft.client.entity.EntityClientPlayerMP;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {

    @Shadow
    private Minecraft mc;

    @Shadow
    //#if MC>=10904
    private NetHandlerPlayClient connection;
    //#else
    //$$ private NetHandlerPlayClient netClientHandler;
    //#endif

    //#if MC>=11200
    @Inject(method = "func_192830_a", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, RecipeBook recipeBook, CallbackInfoReturnable<EntityPlayerSP> ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(mc, worldIn, connection, statisticsManager, recipeBook));
    //#else
    //#if MC>=10904
    //$$ @Inject(method = "createClientPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(mc, worldIn, connection, statisticsManager));
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "func_178892_a", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(mc, worldIn, netClientHandler, statFileWriter));
    //#else
    //$$ @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityClientPlayerMP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(mc, worldIn, mc.getSession(), netClientHandler, statFileWriter));
    //#endif
    //#endif
    //#endif
            ci.cancel();
        }
    }

    //#if MC>=10800
    @Inject(method = "isSpectator", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_isSpectator(CallbackInfoReturnable<Boolean> ci) {
        if (player(mc) instanceof CameraEntity) { // this check should in theory not be required
            ci.setReturnValue(player(mc).isSpectator());
        }
    }
    //#endif

    //#if MC<=10710
    //$$ // Prevent the disconnect GUI from being opened during the short time when the replay is restarted
    //$$ // at which the old network manager is closed but still getting ticked (hence the disconnect GUI opening).
    //$$ @Inject(method = "updateController", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_onlyTickNeverDisconnect(CallbackInfo ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         if (netClientHandler.getNetworkManager().isChannelOpen()) {
    //$$             netClientHandler.getNetworkManager().processReceivedPackets();
    //$$         }
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
