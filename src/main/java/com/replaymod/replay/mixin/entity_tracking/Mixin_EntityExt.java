package com.replaymod.replay.mixin.entity_tracking;

import com.replaymod.replay.ext.EntityExt;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class Mixin_EntityExt implements EntityExt {

    @Shadow
    public float yaw;

    @Shadow
    public float pitch;

    @Unique
    private float trackedYaw = Float.NaN;

    @Unique
    private float trackedPitch = Float.NaN;

    @Override
    public float replaymod$getTrackedYaw() {
        return !Float.isNaN(this.trackedYaw) ? this.trackedYaw : this.yaw;
    }

    @Override
    public float replaymod$getTrackedPitch() {
        return !Float.isNaN(this.trackedPitch) ? this.trackedPitch : this.pitch;
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
