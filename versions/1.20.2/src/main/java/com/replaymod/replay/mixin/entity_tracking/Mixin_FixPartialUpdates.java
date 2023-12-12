package com.replaymod.replay.mixin.entity_tracking;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayNetworkHandler.class)
public class Mixin_FixPartialUpdates {
    // Looks like this has finally been fixed in 1.20.2
}
