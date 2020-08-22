package com.replaymod.recording.mixin;

//#if MC>=10904
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11600
//$$ import net.minecraft.util.registry.RegistryKey;
//$$ import net.minecraft.world.MutableWorldProperties;
//$$ import java.util.function.Supplier;
//#else
import net.minecraft.world.level.LevelProperties;
//#endif

//#if MC>=11400
import net.minecraft.world.chunk.ChunkManager;
//#if MC<11600
import net.minecraft.world.dimension.Dimension;
//#endif
import net.minecraft.world.dimension.DimensionType;
import java.util.function.BiFunction;
//#else
//$$ import net.minecraft.world.storage.ISaveHandler;
//#if MC>=11400
//$$ import net.minecraft.world.dimension.Dimension;
//$$ import net.minecraft.world.storage.WorldSavedDataStorage;
//#else
//$$ import net.minecraft.world.WorldProvider;
//#endif
//#endif


@Mixin(ClientWorld.class)
public abstract class MixinWorldClient extends World implements RecordingEventHandler.RecordingEventSender {
    @Shadow
    private MinecraftClient client;

    //#if MC>=11600
    //$$ protected MixinWorldClient(MutableWorldProperties mutableWorldProperties, RegistryKey<World> registryKey,
                               //#if MC<11602
                               //$$ RegistryKey<DimensionType> registryKey2,
                               //#endif
    //$$                            DimensionType dimensionType, Supplier<Profiler> profiler, boolean bl, boolean bl2, long l) {
    //$$     super(mutableWorldProperties, registryKey,
                //#if MC<11602
                //$$ registryKey2,
                //#endif
    //$$             dimensionType, profiler, bl, bl2, l);
    //$$ }
    //#else
    //#if MC>=11400
    protected MixinWorldClient(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1) {
        super(levelProperties_1, dimensionType_1, biFunction_1, profiler_1, boolean_1);
    }
    //#else
    //$$ protected MixinWorldClient(ISaveHandler saveHandlerIn,
                               //#if MC>=11400
                               //$$ WorldSavedDataStorage mapStorage,
                               //#endif
    //$$                            WorldInfo info,
                               //#if MC>=11400
                               //$$ Dimension providerIn,
                               //#else
                               //$$ WorldProvider providerIn,
                               //#endif
    //$$                            Profiler profilerIn, boolean client) {
    //$$     super(saveHandlerIn,
                //#if MC>=11400
                //$$ mapStorage,
                //#endif
    //$$             info, providerIn, profilerIn, client);
    //$$ }
    //#endif
    //#endif

    private RecordingEventHandler replayModRecording_getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) this.client.worldRenderer).getRecordingEventHandler();
    }

    // Sounds that are emitted by thePlayer no longer take the long way over the server
    // but are instead played directly by the client. The server only sends these sounds to
    // other clients so we have to record them manually.
    // E.g. Block place sounds
    //#if MC>=11400
    //#if FABRIC>=1
    @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
    //$$         at = @At("HEAD"))
    //#endif
    //#else
    //$$ @Inject(method = "playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
    //$$         at = @At("HEAD"))
    //#endif
    public void replayModRecording_recordClientSound(PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category,
                          float volume, float pitch, CallbackInfo ci) {
        if (player == this.client.player) {
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientSound(sound, category, x, y, z, volume, pitch);
            }
        }
    }

    // Same goes for level events (also called effects). E.g. door open, block break, etc.
    //#if MC>=11400
    //#if MC>=11600
    //$$ @Inject(method = "syncWorldEvent", at = @At("HEAD"))
    //#else
    @Inject(method = "playLevelEvent", at = @At("HEAD"))
    //#endif
    private void playLevelEvent (PlayerEntity player, int type, BlockPos pos, int data, CallbackInfo ci) {
    //#else
    //$$ // These are handled in the World class, so we override the method in WorldClient and add our special handling.
    //$$ @Override
    //$$ public void playEvent (EntityPlayer player, int type, BlockPos pos, int data) {
    //#endif
        if (player == this.client.player) {
            // We caused this event, the server won't send it to us
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientEffect(type, pos, data);
            }
        }
        //#if MC<11400
        //$$ super.playEvent(player, type, pos, data);
        //#endif
    }
}
//#endif
