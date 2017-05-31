package com.replaymod.core;

public class Wait extends AbstractTask {
    private final int duration;

    public Wait(int duration) {
        this.duration = duration;
    }

    @Override
    protected void init() {
        new Thread(() -> {
            try {
                Thread.sleep(duration);
                runLater(() -> future.set(null));
            } catch (InterruptedException e) {
                runLater(() -> future.setException(e));
            }
        }).start();
    }
}
