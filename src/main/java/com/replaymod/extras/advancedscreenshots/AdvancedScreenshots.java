package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import net.minecraftforge.common.MinecraftForge;

//#if MC<11300
//$$ import com.replaymod.core.versions.MCVer;
//$$ import com.replaymod.replay.events.ReplayDispatchKeypressesEvent;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.GuiControls;
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=10800
//#if MC>=11300
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

public class AdvancedScreenshots implements Extra {

    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) {
        this.mod = mod;
        MinecraftForge.EVENT_BUS.register(this);
    }

    //#if MC>=11300
    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if (instance != null) {
            instance.takeScreenshot();
        }
    }
    //#else
    //$$ @SubscribeEvent
    //$$ public void onDispatchKeypresses(ReplayDispatchKeypressesEvent.Pre event) {
    //$$     Minecraft mc = MCVer.getMinecraft();
    //$$     if (mc.currentScreen instanceof GuiControls) return;
    //$$     if (!Keyboard.getEventKeyState()) return;
    //$$     if (Keyboard.isRepeatEvent()) return;
    //$$     int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() : Keyboard.getEventKey();
    //$$     if (keyCode == 0 || keyCode != mc.gameSettings.keyBindScreenshot.getKeyCode()) return;
    //$$
    //$$     takeScreenshot();
    //$$
    //$$     event.setCanceled(true);
    //$$ }
    //#endif

    private void takeScreenshot() {
        ReplayMod.instance.runLater(() -> new GuiCreateScreenshot(mod).display());
    }
}
