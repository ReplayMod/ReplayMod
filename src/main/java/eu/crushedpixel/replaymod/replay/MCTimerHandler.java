package eu.crushedpixel.replaymod.replay;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Timer;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.video.ReplayTimer;

public class MCTimerHandler {

	private static Field mcTimer;
	private static Minecraft mc = Minecraft.getMinecraft();

	private static ReplayTimer rpt = new ReplayTimer(20);
	private static Timer timerBefore;

	static {
		try {
			mcTimer = Minecraft.class.getDeclaredField(MCPNames.field("field_71428_T"));
			mcTimer.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setActiveTimer() {
		try {
			if(timerBefore != null) {
				mcTimer.set(mc, timerBefore);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setPassiveTimer() {
		try {
			if(!(getTimer() instanceof ReplayTimer)) {
				timerBefore = getTimer();
				mcTimer.set(mc, rpt);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static int getTicks() {
		try {
			Timer t = getTimer();
			return t.elapsedTicks;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static float getPartialTicks() {
		try {
			Timer t = getTimer();
			return t.elapsedPartialTicks;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static float getRenderTicks() {
		try {
			Timer t = getTimer();
			return t.renderPartialTicks;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	private static Timer getTimer() throws IllegalArgumentException, IllegalAccessException {
		return (Timer)mcTimer.get(mc);
	}

	public static void advanceTicks(int ticks) {
		try {
			Timer t = getTimer();
			t.elapsedTicks += ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void advancePartialTicks(float ticks) {
		try {
			Timer t = getTimer();
			t.elapsedPartialTicks += ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void advanceRenderPartialTicks(float ticks) {
		try {
			Timer t = getTimer();
			t.renderPartialTicks += ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setTimerSpeed(float speed) {
		try {
			Timer t = getTimer();
			t.timerSpeed = speed;
			if(timerBefore != null) {
				timerBefore.timerSpeed = speed;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setRenderPartialTicks(float ticks) {
		try {
			getTimer().renderPartialTicks = ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setPartialTicks(float ticks) {
		try {
			getTimer().elapsedPartialTicks = ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setTicks(int ticks) {
		try {
			getTimer().elapsedTicks = ticks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static float getTimerSpeed() {
		try {
			return getTimer().timerSpeed;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	public static void updateTimer(double d) {
		try {
			Timer t = getTimer();
			double d2 = d;
			//d2 = MathHelper.clamp_double(d2, 0.0D, 1.0D);
			t.elapsedPartialTicks = (float)((double)t.elapsedPartialTicks + d2 * (double)t.timerSpeed * 20);
			t.elapsedTicks = (int)t.elapsedPartialTicks;
			t.elapsedPartialTicks -= (float)t.elapsedTicks;

			if (t.elapsedTicks > 10)
			{
				t.elapsedTicks = 10;
			}

			t.renderPartialTicks = t.elapsedPartialTicks;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
