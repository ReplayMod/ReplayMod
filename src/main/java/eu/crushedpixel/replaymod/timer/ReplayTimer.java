package eu.crushedpixel.replaymod.timer;

import eu.crushedpixel.replaymod.events.handlers.MinecraftTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public class ReplayTimer extends Timer {

    public static ReplayTimer get(Minecraft mc) {
        Timer timer = mc.timer;
        if (!(timer instanceof ReplayTimer)) {
            throw new IllegalStateException("ReplayTimer not installed");
        }
        return (ReplayTimer) timer;
    }

    /**
     * When the timer is set to passive, it does not advance the (render) ticks on it's own.
     */
    public boolean passive;

    public ReplayTimer() {
        super(20);
    }

    @Override
    public void updateTimer() {
        if (!passive) {
            super.updateTimer();
        }

        if (timerSpeed == 0) {
            try {
                MinecraftTicker.runMouseKeyboardTick(Minecraft.getMinecraft());
            } catch (OutOfMemoryError e) {
                // Disable passive mode and reset timer speed so we can use the buttons on the OOM screen
                passive = false;
                timerSpeed = 1;
                throw e;
            }
        }
    }
}
