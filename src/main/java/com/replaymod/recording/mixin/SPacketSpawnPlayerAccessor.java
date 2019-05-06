package com.replaymod.recording.mixin;

import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketSpawnPlayer.class)
public interface SPacketSpawnPlayerAccessor {
    @Accessor("watcher")
    EntityDataManager getDataManager();
    @Accessor("watcher")
    void setDataManager(EntityDataManager value);
}
