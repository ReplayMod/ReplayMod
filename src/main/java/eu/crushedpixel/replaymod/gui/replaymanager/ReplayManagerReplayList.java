package eu.crushedpixel.replaymod.gui.replaymanager;

import eu.crushedpixel.replaymod.gui.GuiReplayListExtended;
import net.minecraft.client.Minecraft;

public class ReplayManagerReplayList extends GuiReplayListExtended {

	private GuiReplayManager parent;
	
	public ReplayManagerReplayList(GuiReplayManager parent, Minecraft mcIn,
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
