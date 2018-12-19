package com.replaymod.replay;

import net.minecraft.client.Minecraft;

public interface ReplaySender {
    int currentTimeStamp();

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public default boolean paused() {
        Minecraft mc = Minecraft.getMinecraft();
        //#if MC>=11200
        return mc.timer.tickLength == Float.POSITIVE_INFINITY;
        //#else
        //$$ return mc.timer.timerSpeed == 0;
        //#endif
    }

    void setReplaySpeed(double factor);
    double getReplaySpeed();

    boolean isAsyncMode();
    void setAsyncMode(boolean async);
    void setSyncModeAndWait();

    void jumpToTime(int value); // async
    void sendPacketsTill(int replayTime); // sync
}
