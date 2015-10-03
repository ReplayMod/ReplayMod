package com.replaymod.replay.gui.screen;

import eu.crushedpixel.replaymod.gui.elements.GuiReplayListExtended;
import net.minecraft.client.Minecraft;

public class ReplayList extends GuiReplayListExtended {

    private GuiReplayViewer parent;

    public ReplayList(GuiReplayViewer parent, Minecraft mcIn,
                      int p_i45010_2_, int p_i45010_3_, int p_i45010_4_, int p_i45010_5_,
                      int p_i45010_6_) {
        super(mcIn, p_i45010_2_, p_i45010_3_, p_i45010_4_, p_i45010_5_,
                p_i45010_6_);

        this.parent = parent;
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick,
                                  int mouseX, int mouseY) {
        super.elementClicked(slotIndex, isDoubleClick, mouseX, mouseY);
        parent.setButtonsEnabled(true);
        if(isDoubleClick) {
            parent.loadReplay(slotIndex);
        }
    }

}
