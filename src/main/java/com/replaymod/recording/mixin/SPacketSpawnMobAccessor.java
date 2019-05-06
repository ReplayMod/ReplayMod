package com.replaymod.recording.mixin;

import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSpawnMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketSpawnMob.class)
public interface SPacketSpawnMobAccessor {
    @Accessor
    EntityDataManager getDataManager();
    @Accessor
    void setDataManager(EntityDataManager value);
}
