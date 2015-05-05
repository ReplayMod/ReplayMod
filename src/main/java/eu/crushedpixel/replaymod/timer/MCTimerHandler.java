package eu.crushedpixel.replaymod.timer;

import eu.crushedpixel.replaymod.video.ReplayTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public class MCTimerHandler {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static ReplayTimer rpt = new ReplayTimer(20);
    private static Timer timerBefore;

    public static void setActiveTimer() {
        if(timerBefore != null) {
            mc.timer = timerBefore;
        }
    }

    public static void setPassiveTimer() {
        if(!(mc.timer instanceof ReplayTimer)) {
            timerBefore = mc.timer;
            mc.timer = rpt;
        }
    }

    public static int getTicks() {
        return mc.timer.elapsedTicks;
    }

    public static void setTicks(int ticks) {
        mc.timer.elapsedTicks = ticks;
    }

    public static float getPartialTicks() {
        return mc.timer.elapsedPartialTicks;
    }

    public static void setPartialTicks(float ticks) {
        mc.timer.elapsedPartialTicks = ticks;
    }

    public static float getRenderTicks() {
        return mc.timer.renderPartialTicks;
    }

    public static void advanceTicks(int ticks) {
        mc.timer.elapsedTicks += ticks;
    }

    public static void advancePartialTicks(float ticks) {
        mc.timer.elapsedPartialTicks += ticks;
    }

    public static void advanceRenderPartialTicks(float ticks) {
        mc.timer.renderPartialTicks += ticks;
    }

    public static void setRenderPartialTicks(float ticks) {
        mc.timer.renderPartialTicks = ticks;
    }

    public static float getTimerSpeed() {
        return mc.timer.timerSpeed;
    }

    public static void setTimerSpeed(float speed) {
        mc.timer.timerSpeed = speed;
        if(timerBefore != null) {
            timerBefore.timerSpeed = speed;
        }
    }

    public static void updateTimer(double d) {
        Timer t = mc.timer;
        //d2 = MathHelper.clamp_double(d2, 0.0D, 1.0D);
        t.elapsedPartialTicks = (float) ((double) t.elapsedPartialTicks + d * (double) t.timerSpeed * 20);
        t.elapsedTicks = (int) t.elapsedPartialTicks;
        t.elapsedPartialTicks -= (float) t.elapsedTicks;

        if(t.elapsedTicks > 10) {
            t.elapsedTicks = 10;
        }

        t.renderPartialTicks = t.elapsedPartialTicks;
    }
}
