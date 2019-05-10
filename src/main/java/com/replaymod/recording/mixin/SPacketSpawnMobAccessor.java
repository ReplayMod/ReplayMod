package com.replaymod.recording.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.client.network.packet.MobSpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobSpawnS2CPacket.class)
public interface SPacketSpawnMobAccessor {
    @Accessor("dataTracker")
    DataTracker getDataManager();
    @Accessor("dataTracker")
    void setDataManager(DataTracker value);
}
