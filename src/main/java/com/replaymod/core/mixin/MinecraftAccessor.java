package com.replaymod.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

//#if MC<11400
import java.util.concurrent.FutureTask;
//#endif

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Timer getTimer();
    @Accessor
    void setTimer(Timer value);

    //#if MC>=11400
    //$$ @Accessor
    //$$ Queue<Runnable> getRenderTaskQueue();
    //#else
    @Accessor
    Queue<FutureTask<?>> getScheduledTasks();
    //#endif

    @Accessor("hasCrashed")
    boolean hasCrashed();

    @Accessor
    CrashReport getCrashReporter();
}
