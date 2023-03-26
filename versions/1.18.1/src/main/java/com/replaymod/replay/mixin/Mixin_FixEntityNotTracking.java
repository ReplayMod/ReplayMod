package com.replaymod.replay.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public class Mixin_FixEntityNotTracking {
    @ModifyVariable(method = { "onEntityPosition", "onEntity", "onEntityPassengersSet" }, at = @At("RETURN"), ordinal = 0)
    private Entity updatePositionIfNotTracked$0(Entity entity) {
        if (entity != null) {
            entity.streamSelfAndPassengers().forEach(this::updatePositionIfNotTracked);
        }
        return entity;
    }

    private void updatePositionIfNotTracked(Entity entity) {
        if (entity != null && entity.world instanceof ClientWorldAccessor world) {
            if (!world.getEntityList().has(entity)) {
                // This only applies to entities that are server-controlled.
                // You wouldn't think this check is necessary but for some stupid reason Spigot sends entities before
                // chunks, so we must not update the entity if it is being ridden by the client player because those
                // will fall into the void and die (client-side only ofc).
                if (entity.isLogicalSideForUpdatingMovement()) {
                    return;
                }

                // Skip interpolation of position updates coming from server
                // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
                int ticks = 0;
                Vec3d prevPos;
                do {
                    prevPos = entity.getPos();
                    if (entity.hasVehicle()) {
                        entity.tickRiding();
                    } else {
                        entity.tick();
                    }
                } while (prevPos.squaredDistanceTo(entity.getPos()) > 0.0001 && ticks++ < 100);
            }
        }
    }
}
