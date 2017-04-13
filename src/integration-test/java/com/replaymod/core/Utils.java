package com.replaymod.core;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class Utils {
    public static <T> void addCallback(ListenableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                onSuccess.accept(result);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                onFailure.accept(t);
            }
        });
    }

    public static void times(int x, Runnable runnable) {
        for (int i = 0; i < x; i++) {
            runnable.run();
        }
    }
}
