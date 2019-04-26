package com.replaymod.recording.mixin;

import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSpawnMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketSpawnMob.class)
public interface SPacketSpawnMobAccessor {
    //#if MC>=10904
    @Accessor
    //#else
    //$$ @Accessor("field_149043_l")
    //#endif
    EntityDataManager getDataManager();
    //#if MC>=10904
    @Accessor
    //#else
    //$$ @Accessor("field_149043_l")
    //#endif
    void setDataManager(EntityDataManager value);
}
