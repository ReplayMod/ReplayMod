package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

import java.io.IOException;

public class GuiMouseInput extends GuiScreen {

    private final GuiReplayOverlay overlay;

    public GuiMouseInput(GuiReplayOverlay overlay) {
        this.overlay = overlay;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        overlay.mouseClicked(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        overlay.mouseReleased(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        overlay.mouseDrag(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for(KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindings)
            ReplayMod.keyInputHandler.handleCustomKeybindings(kb, false, keyCode);

        super.keyTyped(typedChar, keyCode);
    }
}
