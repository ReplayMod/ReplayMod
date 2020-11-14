package com.replaymod.recording.mixin;

import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AddServerScreen.class)
public interface AddServerScreenAccessor {
    @Accessor
    ServerInfo getServer();
}
