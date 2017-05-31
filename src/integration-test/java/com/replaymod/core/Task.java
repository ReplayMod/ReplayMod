package com.replaymod.core;

import com.google.common.util.concurrent.ListenableFuture;

public interface Task {
    ListenableFuture<Void> execute();

    static Task click(int x, int y) {
        return AbstractTask.create(task -> {
            task.click(x, y);
            task.runLater(() -> task.future.set(null));
        });
    }

    static Task drag(int x, int y) {
        return AbstractTask.create(task -> {
            task.drag(x, y);
            task.runLater(() -> task.future.set(null));
        });
    }

    static Task pressKey(int keyCode) {
        return AbstractTask.create(task -> {
            task.press(keyCode);
            task.runLater(() -> task.future.set(null));
        });
    }

    static Task pressKey(char character, int keyCode) {
        return AbstractTask.create(task -> {
            task.press(character, keyCode);
            task.runLater(() -> task.future.set(null));
        });
    }
}
