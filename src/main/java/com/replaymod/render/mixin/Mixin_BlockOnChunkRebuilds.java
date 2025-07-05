package com.replaymod.render.mixin;

//#if MC>=11500
import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.thread.TaskExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//#if MC>=12106
//$$ import net.minecraft.client.render.chunk.AbstractChunkRenderData;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import java.util.concurrent.Executor;
//#endif

//#if MC>=12105
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import java.util.AbstractQueue;
//$$ import java.util.Iterator;
//#endif

//#if MC>=12102
//$$ import net.minecraft.util.thread.SimpleConsecutiveExecutor;
//#endif

//#if MC>=12003
//$$ import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
//#endif

@Mixin(ChunkBuilder.class)
public abstract class Mixin_BlockOnChunkRebuilds implements ForceChunkLoadingHook.IBlockOnChunkRebuilds {
    //#if MC>=12003
    //$$ @Shadow @Final private BlockBufferBuilderPool buffersPool;
    //$$ @Unique
    //$$ private int getAvailableBufferCount() {
    //$$     return this.buffersPool.getAvailableBuilderCount();
    //$$ }
    //#else
    @Shadow @Final private Queue<BlockBufferBuilderStorage> threadBuffers;
    @Unique
    private int getAvailableBufferCount() {
        return threadBuffers.size();
    }
    //#endif

    //#if MC>=11800
    //$$ @Unique
    //$$ private boolean upload() {
    //$$     boolean anything = false;
    //$$     Runnable runnable;
    //$$     while ((runnable = this.uploadQueue.poll()) != null) {
    //$$         runnable.run();
    //$$         anything = true;
    //$$     }
        //#if MC>=12106
        //$$ AbstractChunkRenderData entry;
        //$$     while ((entry = this.renderQueue.poll()) != null) {
        //$$     entry.close();
        //$$     anything = true;
        //$$ }
        //#endif
    //$$     return anything;
    //$$ }
    //#else
    @Shadow public abstract boolean upload();
    //#endif

    //#if MC>=12102
    //$$ @Shadow @Final private SimpleConsecutiveExecutor consecutiveExecutor;
    //#else
    @Shadow @Final private TaskExecutor<Runnable> mailbox;
    //#endif

    @Shadow protected abstract void scheduleRunTasks();

    //#if MC>=12105
    //$$ @Mutable
    //#endif
    @Shadow @Final private Queue<Runnable> uploadQueue;
    private final Lock waitingForWorkLock = new ReentrantLock();
    private final Condition newWork = waitingForWorkLock.newCondition();
    private volatile boolean allDone;

    private int totalBufferCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rememberTotalThreads(CallbackInfo ci) {
        this.totalBufferCount = getAvailableBufferCount();
    }

    @Inject(method = "scheduleRunTasks", at = @At("RETURN"))
    private void notifyMainThreadIfEverythingIsDone(CallbackInfo ci) {
        if (getAvailableBufferCount() == this.totalBufferCount) {
            // Looks like we're done, better notify the main thread in case the previous task didn't generate an upload
            this.waitingForWorkLock.lock();
            try {
                this.allDone = true;
                this.newWork.signalAll();
            } finally {
                this.waitingForWorkLock.unlock();
            }
        } else {
            this.allDone = false;
        }
    }

    //#if MC>=12106
    //$$ @Shadow @Final @Mutable
    //$$ Executor uploadExecutor;
    //$$ @Shadow @Final @Mutable Queue<AbstractChunkRenderData> renderQueue;
    //$$ @Inject(method = "<init>", at = @At("RETURN"))
    //$$ private void notifyMainThreadOfNewUpload(CallbackInfo ci) {
    //$$     Executor innerUploadExecutor = this.uploadExecutor;
    //$$     this.uploadExecutor = runnable -> {
    //$$         innerUploadExecutor.execute(runnable);
    //$$         waitingForWorkLock.lock();
    //$$         try {
    //$$             newWork.signal();
    //$$         } finally {
    //$$             waitingForWorkLock.unlock();
    //$$         }
    //$$     };
    //$$     Queue<AbstractChunkRenderData> innerRenderQueue = this.renderQueue;
    //$$     this.renderQueue = new AbstractQueue<>() {
    //$$         @Override
    //$$         public boolean offer(AbstractChunkRenderData runnable) {
    //$$             boolean result = innerRenderQueue.offer(runnable);
    //$$             waitingForWorkLock.lock();
    //$$             try {
    //$$                 newWork.signal();
    //$$             } finally {
    //$$                 waitingForWorkLock.unlock();
    //$$             }
    //$$             return result;
    //$$         }
    //$$         @Override public AbstractChunkRenderData poll() { return innerRenderQueue.poll(); }
    //$$         @Override public AbstractChunkRenderData peek() { return innerRenderQueue.peek(); }
    //$$         @Override public int size() { return innerRenderQueue.size(); }
    //$$         @Override public Iterator<AbstractChunkRenderData> iterator() { return innerRenderQueue.iterator(); }
    //$$     };
    //$$ }
    //#elseif MC>=12105
    //$$ @Inject(method = "<init>", at = @At("RETURN"))
    //$$ private void notifyMainThreadOfNewUpload(CallbackInfo ci) {
    //$$     Queue<Runnable> inner = this.uploadQueue;
    //$$     this.uploadQueue = new AbstractQueue<>() {
    //$$         @Override
    //$$         public boolean offer(Runnable runnable) {
    //$$             boolean result = inner.offer(runnable);
    //$$             waitingForWorkLock.lock();
    //$$             try {
    //$$                 newWork.signal();
    //$$             } finally {
    //$$                 waitingForWorkLock.unlock();
    //$$             }
    //$$             return result;
    //$$         }
    //$$         @Override public Runnable poll() { return inner.poll(); }
    //$$         @Override public Runnable peek() { return inner.peek(); }
    //$$         @Override public int size() { return inner.size(); }
    //$$         @Override public Iterator<Runnable> iterator() { return inner.iterator(); }
    //$$     };
    //$$ }
    //#else
    @Inject(method = "scheduleUpload", at = @At("RETURN"))
    private void notifyMainThreadOfNewUpload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        this.waitingForWorkLock.lock();
        try {
            this.newWork.signal();
        } finally {
            this.waitingForWorkLock.unlock();
        }
    }
    //#endif

    private boolean waitForMainThreadWork() {
        //#if MC>=12102
        //$$ this.consecutiveExecutor.executeAsync(future -> {
        //$$     scheduleRunTasks();
        //$$     future.complete(getAvailableBufferCount() == this.totalBufferCount);
        //$$ }).join();
        //#else
        boolean allDone = this.mailbox.<Boolean>ask(reply -> () -> {
            scheduleRunTasks();
            reply.send(getAvailableBufferCount() == this.totalBufferCount);
        }).join();
        //#endif

        if (allDone) {
            return true;
        } else {
            this.waitingForWorkLock.lock();
            try {
                while (true) {
                    //#if MC<11900
                    // Now, what is this call doing here you might be wondering. Well, from a quick look over everything
                    // it does not look like it would be required but have a **very** close look at [scheduleUpload]:
                    // It is not actually guaranteed to run the upload on the main thread, it just looks like it (and
                    // was probably supposed to do that) but in actuality, because it adds the first, empty future to
                    // the upload queue before attaching the thenCompose callback, that future can actually be completed
                    // by the main thread before thenCompose is called, resulting in thenCompose immediately calling
                    // its callback on the same (non-main) thread.
                    // Looking at how the upload behaves executed on a non-main thread, it eventually just enqueues
                    // itself in the RenderSystem's queue and returns a future for that.
                    // So, even though our [notifyMainThreadOfNewUpload] gets executed after all that, we would simply
                    // dead-lock ourselves here (since the upload queue is already empty), if we did never do this call
                    // to run the upload scheduled via this particular path of code execution.
                    RenderSystem.replayQueue();
                    //#endif

                    if (this.allDone) {
                        return true;
                    } else if (!this.uploadQueue.isEmpty()) {
                        return false;
                    } else {
                        this.newWork.awaitUninterruptibly();
                    }
                }
            } finally {
                this.waitingForWorkLock.unlock();
            }
        }
    }

    @Override
    public boolean uploadEverythingBlocking() {
        boolean anything = false;

        boolean allChunksBuilt;
        do {
            allChunksBuilt = waitForMainThreadWork();
            while (upload()) {
                anything = true;
            }
        } while (!allChunksBuilt);

        return anything;
    }
}
//#endif
