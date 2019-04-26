package com.replaymod.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;
import java.util.concurrent.FutureTask;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Timer getTimer();
    @Accessor
    void setTimer(Timer value);

    @Accessor
    Queue<FutureTask<?>> getScheduledTasks();

    @Accessor("hasCrashed")
    boolean hasCrashed();

    @Accessor
    CrashReport getCrashReporter();
}
