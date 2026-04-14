package com.replaymod.core.mixin;

import net.minecraft.CrashReport;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(BlockableEventLoop.class)
public interface BlockableEventLoopAccessor {
    @Accessor
    Supplier<CrashReport> getDelayedCrash();
}
