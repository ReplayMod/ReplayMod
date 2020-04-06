package com.replaymod.recording.mixin;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    // TODO probably https://github.com/ReplayMod/remap/issues/10
    //#if MC>=11500
    @Accessor("paused")
    //#else
    //$$ @Accessor("field_5524")
    //#endif
    boolean isGamePaused();
}
