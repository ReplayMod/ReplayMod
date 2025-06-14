package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12106
//$$ import net.minecraft.util.PlayerInput;
//#endif

//#if MC>=11400
import net.minecraft.client.world.ClientWorld;
//#else
//$$ import net.minecraft.world.World;
//#endif

//#if MC>=11200
//#if MC>=11400
import net.minecraft.client.recipebook.ClientRecipeBook;
//#else
//$$ import net.minecraft.stats.RecipeBook;
//#endif
//#endif
//#if MC>=10904
import net.minecraft.stat.StatHandler;
//#else
//$$ import net.minecraft.stats.StatFileWriter;
//#endif

//#if MC>=10800
import net.minecraft.client.network.ClientPlayerEntity;
//#else
//$$ import net.minecraft.client.entity.EntityClientPlayerMP;
//#endif


@Mixin(ClientPlayerInteractionManager.class)
public abstract class Mixin_CreateReplayCamera {

    @Shadow
    private MinecraftClient client;

    @Shadow
    //#if MC>=10904
    private ClientPlayNetworkHandler networkHandler;
    //#else
    //$$ private NetHandlerPlayClient netClientHandler;
    //#endif

    //#if MC>=11400
    //#if MC>=12106
    //$$ @Inject(method = "createPlayer(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;Lnet/minecraft/util/PlayerInput;Z)Lnet/minecraft/client/network/ClientPlayerEntity;", at=@At("HEAD"), cancellable = true)
    //#elseif MC>=11602
    @Inject(method = "createPlayer(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;ZZ)Lnet/minecraft/client/network/ClientPlayerEntity;", at=@At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    //#endif
    private void replayModReplay_createReplayCamera(
            //#if MC>=11400
            ClientWorld worldIn,
            //#else
            //$$ World worldIn,
            //#endif
            StatHandler statisticsManager,
            ClientRecipeBook recipeBookClient,
            //#if MC>=11600
            //#if MC>=12106
            //$$ PlayerInput playerInput,
            //#else
            boolean lastIsHoldingSneakKey,
            //#endif
            boolean lastSprinting,
            //#endif
            CallbackInfoReturnable<ClientPlayerEntity> ci
    ) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(this.client, worldIn, this.networkHandler, statisticsManager, recipeBookClient));
    //#else
    //#if MC>=11200
    //$$ @Inject(method = "func_192830_a", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, RecipeBook recipeBook, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.connection, statisticsManager, recipeBook));
    //#else
    //#if MC>=10904
    //$$ @Inject(method = "createClientPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.connection, statisticsManager));
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "func_178892_a", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.netClientHandler, statFileWriter));
    //#else
    //$$ @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityClientPlayerMP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.mc.getSession(), this.netClientHandler, statFileWriter));
    //#endif
    //#endif
    //#endif
    //#endif
            ci.cancel();
        }
    }
}
