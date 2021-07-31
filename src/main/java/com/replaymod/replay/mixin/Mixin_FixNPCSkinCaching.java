package com.replaymod.replay.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class Mixin_FixNPCSkinCaching {
    @Shadow @Nullable protected abstract PlayerListEntry getPlayerListEntry();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void forceCachePlayerListEntry(CallbackInfo ci) {
        // Server-side NPCs (without a client mod) rely on the player list entry being cached in this class to function
        // because the remove the entry shortly after spawning the player. However this method is only called when the
        // player is rendered, and if we are currently jumping around in a replay then it most likely will not be
        // rendered while the entry is still there. The result is that most NPCs get Steve/Alex skins instead of the
        // intended ones. To fix that, we make this caching-glitch which servers have come to rely on an actual feature
        // by just fetching the cache in the constructor which arguably is what MC should have done to begin with,
        // especially because the spawn packet handling code already requires the entry to be present).
        if (MinecraftClient.getInstance().getNetworkHandler() != null) { // will be null if this is the client player
            this.getPlayerListEntry();
        }
    }
}
