package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(SectionRenderDispatcher.class)
public abstract class Mixin_BlockOnChunkRebuilds implements ForceChunkLoadingHook.IBlockOnChunkRebuilds {
    @Unique
    private final ReentrantLock newMainThreadWorkLock = new ReentrantLock();
    @Unique
    private final Condition newMainThreadWork = newMainThreadWorkLock.newCondition();

    @Unique
    private long scheduledTasks = 0;
    @Unique
    private long doneTasks = 0;

    @Inject(method = "schedule", at = @At(value = "INVOKE", target = "Lnet/minecraft/TracingExecutor;execute(Ljava/lang/Runnable;)V"))
    private void incrementScheduledTasks(CallbackInfo ci) {
        newMainThreadWorkLock.lock();
        try {
            scheduledTasks++;
        } finally {
            newMainThreadWorkLock.unlock();
        }
    }

    @WrapMethod(method = "runTask")
    private void incrementDoneTasks(Operation<Void> original) {
        try {
            original.call();
        } finally {
            newMainThreadWorkLock.lock();
            try {
                doneTasks++;
                newMainThreadWork.signalAll();
            } finally {
                newMainThreadWorkLock.unlock();
            }
        }
    }

    @Unique
    private long prevTotalDoneTasks;

    @Override
    public boolean uploadEverythingBlocking() {
        while (true) {
            long doneTasksAtStartOfLoop;
            boolean allTasksDone;
            newMainThreadWorkLock.lock();
            try {
                doneTasksAtStartOfLoop = doneTasks;
                allTasksDone = scheduledTasks == doneTasks;
            } finally {
                newMainThreadWorkLock.unlock();
            }

            // Upload any buffers that may still be pending
            uploadAllStagedAllocations();

            // If there were no tasks still running, everything should be uploaded now.
            if (allTasksDone) {
                break;
            }

            // Otherwise we need to wait for more work to come in
            newMainThreadWorkLock.lock();
            try {
                while (doneTasksAtStartOfLoop == doneTasks) {
                    newMainThreadWork.awaitUninterruptibly();
                }
            } finally {
                newMainThreadWorkLock.unlock();
            }
        }

        boolean doneAnything = doneTasks != prevTotalDoneTasks;
        prevTotalDoneTasks = doneTasks;
        return doneAnything;
    }

    @Unique
    private void uploadAllStagedAllocations() {
        copyLock.lock();
        try {
            // MC will only allow one buffer resize per frame (presumably to avoid lag spikes), so we'll simply call
            // the method as often as there are buffers (there's only a fixed amount of 3, one per ChunkSectionLayer)
            for (int i = 0; i < chunkUberBuffers.size(); i++) {
                uploadGlobalGeomBuffersToGPU();
            }
        } finally {
            copyLock.unlock();
        }
    }

    @Shadow
    @Final
    private ReentrantLock copyLock;

    @Shadow
    public abstract void uploadGlobalGeomBuffersToGPU();

    @Shadow
    @Final
    private Map<ChunkSectionLayer, ?> chunkUberBuffers;
}
