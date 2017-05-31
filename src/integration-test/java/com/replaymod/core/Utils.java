package com.replaymod.core;

public class Utils {
    public static void times(int x, Runnable runnable) {
        for (int i = 0; i < x; i++) {
            runnable.run();
        }
    }
}
