package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import com.replaymod.replay.events.ReplayDispatchKeypressesEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

public class AdvancedScreenshots implements Extra {

    private ReplayMod mod;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void register(ReplayMod mod) throws Exception {
        this.mod = mod;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onDispatchKeypresses(ReplayDispatchKeypressesEvent.Pre event) {
        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() : Keyboard.getEventKey();

        // all the conditions required to trigger a screenshot condensed in a single if statement
        if (keyCode != 0 && !Keyboard.isRepeatEvent()
                && (!(mc.currentScreen instanceof GuiControls) || ((GuiControls) mc.currentScreen).time <= mc.getSystemTime() - 20L)
                && Keyboard.getEventKeyState()
                && keyCode == mc.gameSettings.keyBindScreenshot.getKeyCode()) {

            ReplayMod.instance.runLater(() -> {
                new GuiCreateScreenshot(mod).display();
            });

            event.setCanceled(true);
        }
    }
}
