package eu.crushedpixel.replaymod.gui;

import com.replaymod.core.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class GuiReplaySaving extends GuiScreen {

    private GuiScreen waiting = null;

    private final Minecraft mc = Minecraft.getMinecraft();

    public GuiReplaySaving(GuiScreen waiting) {
        this.waiting = waiting;
        ReplayMod.replayFileAppender.addFinishListener(this);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        ReplayMod.replayFileAppender.callListeners();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.replaysaving.title"), this.width / 2, 20, 16777215);
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.replaysaving.message"), this.width / 2, 40, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void dispatch() {
        mc.displayGuiScreen(waiting);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        //Ignore key inputs to disallow users from closing this GUI
    }
}
