package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;

public class LightingHandler {

    private static float initialGamma = 0;

    private static boolean enabled = false;

    //TODO: Properly reset Gamma on game start
    //TODO: Properly handle manual gamma changes while in Replay
    public static void setLighting(boolean lighting) {
        if(lighting) {
            if(!enabled) {
                initialGamma = Minecraft.getMinecraft().gameSettings.getOptionFloatValue(Options.GAMMA);
            }
            Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, 1000);
        } else Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, initialGamma);

        enabled = lighting;

        try {
            if(ReplayMod.replaySender.paused()) {
                MCTimerHandler.advancePartialTicks(1);
                MCTimerHandler.advanceRenderPartialTicks(1);
            } else {
                Minecraft.getMinecraft().entityRenderer.updateCameraAndRender(0);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
