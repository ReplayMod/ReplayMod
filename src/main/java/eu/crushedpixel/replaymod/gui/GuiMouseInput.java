package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

import java.io.IOException;

public class GuiMouseInput extends GuiScreen {

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for(KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindings)
            ReplayMod.keyInputHandler.handleCustomKeybindings(kb, false, keyCode);

        super.keyTyped(typedChar, keyCode);
    }
}
