package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.extras.Extra;
import com.replaymod.replay.events.ReplayDispatchKeypressesEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraftforge.common.MinecraftForge;

//#if MC>=11300
import com.replaymod.core.versions.MCVer.Keyboard;
import net.minecraftforge.client.event.GuiScreenEvent;
//#else
//$$ import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

public class AdvancedScreenshots implements Extra {

    private ReplayMod mod;

    private final Minecraft mc = MCVer.getMinecraft();

    @Override
    public void register(ReplayMod mod) throws Exception {
        this.mod = mod;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onDispatchKeypresses(ReplayDispatchKeypressesEvent.Pre event) {
        if (mc.currentScreen instanceof GuiControls) return;
        //#if MC>=11300
        // FIXME
        //#else
        //$$ if (!Keyboard.getEventKeyState()) return;
        //$$ if (Keyboard.isRepeatEvent()) return;
        //$$ int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() : Keyboard.getEventKey();
        //$$ if (keyCode == 0 || keyCode != mc.gameSettings.keyBindScreenshot.getKeyCode()) return;
        //#endif

        ReplayMod.instance.runLater(() -> {
            new GuiCreateScreenshot(mod).display();
        });

        event.setCanceled(true);
    }
}
