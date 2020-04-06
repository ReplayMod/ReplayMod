package com.replaymod.recording.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerSpawnS2CPacket.class)
public interface SPacketSpawnPlayerAccessor {
    //#if MC<11500
    //$$ @Accessor("dataTracker")
    //$$ DataTracker getDataManager();
    //$$ @Accessor("dataTracker")
    //$$ void setDataManager(DataTracker value);
    //#endif
}
