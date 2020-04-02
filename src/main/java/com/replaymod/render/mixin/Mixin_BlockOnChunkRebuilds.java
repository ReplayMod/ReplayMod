package com.replaymod.render.mixin;

//#if MC>=11500
//$$ import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
//$$ import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
//$$ import net.minecraft.client.render.chunk.ChunkBuilder;
//$$ import net.minecraft.util.thread.TaskExecutor;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ import java.util.Queue;
//$$ import java.util.concurrent.CompletableFuture;
//$$ import java.util.concurrent.locks.Condition;
//$$ import java.util.concurrent.locks.Lock;
//$$ import java.util.concurrent.locks.ReentrantLock;
//$$
//$$ @Mixin(ChunkBuilder.class)
//$$ public abstract class Mixin_BlockOnChunkRebuilds implements ChunkLoadingRenderGlobal.IBlockOnChunkRebuilds {
//$$     @Shadow @Final private Queue<BlockBufferBuilderStorage> threadBuffers;
//$$
//$$     @Shadow public abstract boolean upload();
//$$
//$$     @Shadow @Final private TaskExecutor<Runnable> mailbox;
//$$
//$$     @Shadow protected abstract void scheduleRunTasks();
//$$
//$$     @Shadow @Final private Queue<Runnable> uploadQueue;
//$$     private final Lock waitingForWorkLock = new ReentrantLock();
//$$     private final Condition newWork = waitingForWorkLock.newCondition();
//$$     private volatile boolean allDone;
//$$
//$$     private int totalBufferCount;
//$$
//$$     @Inject(method = "<init>", at = @At("RETURN"))
//$$     private void rememberTotalThreads(CallbackInfo ci) {
//$$         this.totalBufferCount = this.threadBuffers.size();
//$$     }
//$$
//$$     @Inject(method = "scheduleRunTasks", at = @At("RETURN"))
//$$     private void notifyMainThreadIfEverythingIsDone(CallbackInfo ci) {
//$$         if (this.threadBuffers.size() == this.totalBufferCount) {
//$$             // Looks like we're done, better notify the main thread in case the previous task didn't generate an upload
//$$             this.waitingForWorkLock.lock();
//$$             try {
//$$                 this.allDone = true;
//$$                 this.newWork.signalAll();
//$$             } finally {
//$$                 this.waitingForWorkLock.unlock();
//$$             }
//$$         } else {
//$$             this.allDone = false;
//$$         }
//$$     }
//$$
//$$     @Inject(method = "scheduleUpload", at = @At("RETURN"))
//$$     private void notifyMainThreadOfNewUpload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
//$$         this.waitingForWorkLock.lock();
//$$         try {
//$$             this.newWork.signal();
//$$         } finally {
//$$             this.waitingForWorkLock.unlock();
//$$         }
//$$     }
//$$
//$$     private boolean waitForMainThreadWork() {
//$$         boolean allDone = this.mailbox.<Boolean>ask(reply -> () -> {
//$$             scheduleRunTasks();
//$$             reply.send(this.threadBuffers.size() == this.totalBufferCount);
//$$         }).join();
//$$
//$$         if (allDone) {
//$$             return true;
//$$         } else {
//$$             this.waitingForWorkLock.lock();
//$$             try {
//$$                 while (true) {
//$$                     if (this.allDone) {
//$$                         return true;
//$$                     } else if (!this.uploadQueue.isEmpty()) {
//$$                         return false;
//$$                     } else {
//$$                         this.newWork.awaitUninterruptibly();
//$$                     }
//$$                 }
//$$             } finally {
//$$                 this.waitingForWorkLock.unlock();
//$$             }
//$$         }
//$$     }
//$$
//$$     @Override
//$$     public boolean uploadEverythingBlocking() {
//$$         boolean anything = false;
//$$
//$$         boolean allChunksBuilt;
//$$         do {
//$$             allChunksBuilt = waitForMainThreadWork();
//$$             while (upload()) {
//$$                 anything = true;
//$$             }
//$$         } while (!allChunksBuilt);
//$$
//$$         return anything;
//$$     }
//$$ }
//#endif
