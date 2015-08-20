package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiMouseInput extends GuiScreen {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final GuiReplayOverlay overlay;

    private boolean shouldClose = false;

    public GuiMouseInput(GuiReplayOverlay overlay) {
        this.overlay = overlay;
        Mouse.setGrabbed(false);
        Mouse.setCursorPosition(mc.displayWidth/2, mc.displayHeight/2);

        if(mc.currentScreen instanceof GuiMouseInput) {
            shouldClose = true;
        }
    }

    @Override
    public void initGui() {
        if(shouldClose) mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        overlay.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        overlay.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        overlay.mouseDrag(mouseX, mouseY, clickedMouseButton);
    }

    @Override
    public void onGuiClosed() {
        ReplayMod.overlay.closeToolbar();
    }
}
