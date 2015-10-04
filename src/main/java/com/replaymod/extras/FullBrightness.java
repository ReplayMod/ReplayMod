package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class FullBrightness implements Extra {
    private GameSettings gameSettings;
    private boolean active;
    private float originalGamma;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.gameSettings = mod.getMinecraft().gameSettings;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                mod.getMinecraft().entityRenderer.lightmapUpdateNeeded = true;
            }
        });

        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void preRender(TickEvent.RenderTickEvent event) {
        if (active) {
            if (event.phase == TickEvent.Phase.START) {
                originalGamma = gameSettings.gammaSetting;
                gameSettings.gammaSetting = 1000;
            } else if (event.phase == TickEvent.Phase.END) {
                gameSettings.gammaSetting = originalGamma;
            }
        }
    }
}
