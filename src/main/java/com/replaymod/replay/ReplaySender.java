package com.replaymod.replay;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import net.minecraft.client.Minecraft;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public interface ReplaySender {
    int currentTimeStamp();

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public default boolean paused() {
        Minecraft mc = getMinecraft();
        TimerAccessor timer = (TimerAccessor) ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11200
        return timer.getTickLength() == Float.POSITIVE_INFINITY;
        //#else
        //$$ return timer.getTimerSpeed() == 0;
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
