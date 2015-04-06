package eu.crushedpixel.replaymod.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.util.Timer;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;

public class LightingHandler {

	private static float initialGamma = 0;
	
	private static boolean enabled = false;
	
	public static void setInitialGamma(float gamma) {
		initialGamma = gamma;
	}
	
	public static void setLighting(boolean lighting) {
		if(lighting) {
			if(!enabled) {
				initialGamma = Minecraft.getMinecraft().gameSettings.getOptionFloatValue(Options.GAMMA);
			}
			Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, 1000);
		}
		else Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, initialGamma);
		
		enabled = lighting;
		
		try {
			if(ReplayHandler.isPaused()) {
				MCTimerHandler.advancePartialTicks(1);
				MCTimerHandler.advanceRenderPartialTicks(1);
			} else {
				Minecraft.getMinecraft().entityRenderer.updateCameraAndRender(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
