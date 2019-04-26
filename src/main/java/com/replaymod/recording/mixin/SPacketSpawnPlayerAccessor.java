package com.replaymod.recording.mixin;

import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketSpawnPlayer.class)
public interface SPacketSpawnPlayerAccessor {
    //#if MC>=10809
    @Accessor("watcher")
    //#else
    //$$ @Accessor("field_148960_i")
    //#endif
    EntityDataManager getDataManager();
    //#if MC>=10809
    @Accessor("watcher")
    //#else
    //$$ @Accessor("field_148960_i")
    //#endif
    void setDataManager(EntityDataManager value);
}
