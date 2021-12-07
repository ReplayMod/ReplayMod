package com.replaymod.replay.mixin.entity_tracking;

import com.replaymod.replay.ext.EntityExt;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class Mixin_EntityExt implements EntityExt {

    @Unique
    private float trackedYaw;

    @Unique
    private float trackedPitch;

    @Override
    public float replaymod$getTrackedYaw() {
        return this.trackedYaw;
    }

    @Override
    public float replaymod$getTrackedPitch() {
        return this.trackedPitch;
    }

    @Override
    public void replaymod$setTrackedYaw(float value) {
        this.trackedYaw = value;
    }

    @Override
    public void replaymod$setTrackedPitch(float value) {
        this.trackedPitch = value;
    }
}
