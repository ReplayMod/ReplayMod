package com.replaymod.replay.mixin;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient {

    // Looks like this has finally been fixed in 1.14 (or been moved somewhere else entirely, guess we'll find out)
    //#if MC<11400
    /**
     * Fixes a bug in vanilla Minecraft that leaves entities remaining in the entityList even after respawn.
     * The entityList in WorldClient is a Set that assumes that the entity ID of its entities does not change.
     * However in {@link WorldClient#addEntityToWorld(int, Entity)}, the entity passed in is first added to the
     * set and subsequently its id is changed, leaving it stuck in he Set. That is the buggy method.
     * This wouldn't be too much of a problem if the entity had the correct id to begin with, however the handler for
     * the spawn player packet creates an EntityOtherPlayerMP which takes its id from a counter and then passes it to
     * this method with the wrong id.
     * This mixin fixes the id of the entity before it is added to the set instead of right after.
     * The original id change right after is not changed, however it should not have any effect.
     * @param entityId The id to be set for the entity
     * @param entity The entity to be added
     * @param ci Callback info
     */
    @Inject(method = "addEntityToWorld", at=@At("HEAD"))
    public void replayModReplay_fix_addEntityToWorld(int entityId, Entity entity, CallbackInfo ci) {
        entity.setEntityId(entityId);
    }
    //#endif
}
