package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;

public class LightingHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static float initialGamma = 0;

    private static boolean enabled = false;

    public static void setLighting(boolean lighting) {

        float gamma = mc.gameSettings.getOptionFloatValue(Options.GAMMA);
        if(gamma < 1000) {
            initialGamma = gamma;
        }

        if(lighting) mc.gameSettings.setOptionFloatValue(Options.GAMMA, 1000);
        else mc.gameSettings.setOptionFloatValue(Options.GAMMA, initialGamma);

        enabled = lighting;

        if(ReplayMod.replaySender.paused()) {
            mc.entityRenderer.lightmapUpdateNeeded = true;
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                mc.gameSettings.gammaSetting = initialGamma;
                mc.gameSettings.saveOptions();
            }
        }, "lighting-handler-shutdown-hook"));
    }

}
