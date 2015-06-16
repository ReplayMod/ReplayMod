package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;

public class LightingHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static float initialGamma = 0;

    private static boolean enabled = false;
    
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
                mc.timer.elapsedPartialTicks += 1;
                mc.timer.renderPartialTicks += 1;
            } else {
                Minecraft.getMinecraft().entityRenderer.updateCameraAndRender(0);
            }
        } catch(Exception e) {
            e.printStackTrace();
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
