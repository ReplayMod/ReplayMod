package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.replay.ReplayProcess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;

public class GuiCancelRender extends GuiYesNo {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static GuiYesNoCallback callback = new GuiYesNoCallback() {

        @Override
        public void confirmClicked(boolean result, int id) {
            if(result) {
                ReplayProcess.stopReplayProcess(false);
            }
            mc.displayGuiScreen(null);
        }
    };

    public GuiCancelRender() {
        super(callback, I18n.format("replaymod.gui.cancelrender.title"),
                I18n.format("replaymod.gui.cancelrender.message"), 0);
    }

}
