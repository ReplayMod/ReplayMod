package com.replaymod.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import static com.replaymod.core.ReplayModIntegrationTest.LOGGER;

public class CompositeTask implements Task {
    private SettableFuture<Void> future;
    protected final Task[] children;

    public CompositeTask(Task[] children) {
        this.children = children;
    }

    @Override
    public ListenableFuture<Void> execute() {
        future = SettableFuture.create();
        executeChild(0);
        return future;
    }

    private void executeChild(int childIndex) {
        if (future.isDone()) return;
        if (childIndex < children.length) {
            ReplayMod.instance.runLater(() -> {
                try {
                    Task task = children[childIndex];
                    LOGGER.info("Running task {}", task);
                    ListenableFuture<Void> childFuture = task.execute();
                    Utils.addCallback(childFuture, done -> executeChild(childIndex + 1), err -> future.setException(err));
                } catch (Throwable t) {
                    future.setException(t);
                }
            });
        } else {
            future.set(null);
        }
    }
}
