package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.gui.elements.GuiReplayListExtended;
import net.minecraft.client.Minecraft;

public class ReplayFileList extends GuiReplayListExtended {

    private GuiReplayCenter parent;

    public ReplayFileList(Minecraft mcIn, int p_i45010_2_, int p_i45010_3_,
                          int p_i45010_4_, int p_i45010_5_, GuiReplayCenter parent) {
        super(mcIn, p_i45010_2_, p_i45010_3_, p_i45010_4_, p_i45010_5_, 50);
        this.parent = parent;
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        super.elementClicked(slotIndex, isDoubleClick, mouseX, mouseY);
        parent.elementSelected(slotIndex);
    }
}
