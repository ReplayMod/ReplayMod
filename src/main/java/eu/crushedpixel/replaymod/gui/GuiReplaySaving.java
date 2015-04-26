package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiReplaySaving extends GuiScreen {

    public static boolean replaySaving = false;

    private GuiScreen waiting = null;

    public GuiReplaySaving(GuiScreen waiting) {
        this.waiting = waiting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.replaysaving.title"), this.width / 2, 20, 16777215);
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.replaysaving.message"), this.width / 2, 40, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if(!replaySaving) {
            Minecraft.getMinecraft().displayGuiScreen(waiting);
        }
    }

}
