package com.replaymod.replay.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
    //#if MC>=11800
    //$$ @Accessor
    //$$ net.minecraft.world.EntityList getEntityList();
    //#endif
}
