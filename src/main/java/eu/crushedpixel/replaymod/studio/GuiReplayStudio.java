package eu.crushedpixel.replaymod.studio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import de.johni0702.replaystudio.studio.ReplayStudio;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.GuiDropdown;

public class GuiReplayStudio extends GuiScreen {
	
	private GuiDropdown replayDropdown;
	
	private ReplayStudio studio = new ReplayStudio();
	
	@Override
	public void initGui() {
		List<GuiButton> tabButtons = new ArrayList<GuiButton>();
		
		tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_TRIM_TAB, 0, 0, "Trim Replay"));
		tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_CONNECT_TAB, 0, 0, "Connect Replays"));
		tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_MODIFY_TAB, 0, 0, "Modify Replay"));
		
		int w = this.width - 30;
		int w2 = w/tabButtons.size();
		int i = 0;
		for(GuiButton b : tabButtons) {
			int x = 15+(w2*i);
			b.xPosition = x+2;
			b.yPosition = 30;
			b.width = w2-4;

			buttonList.add(b);

			i++;
		}
		
		replayDropdown = new GuiDropdown(1, fontRendererObj, 15+2+1, 60, 100);
		replayDropdown.addElement("test1");
		replayDropdown.addElement("test2");
		replayDropdown.addElement("test3");
		replayDropdown.addElement("test4");
		replayDropdown.addElement("test5");
		replayDropdown.addElement("test6");
		replayDropdown.addElement("test7");
		replayDropdown.addElement("test8");
		replayDropdown.addElement("test9");
	};
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
			throws IOException {
		replayDropdown.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Replay Studio", this.width / 2, 10, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
		replayDropdown.drawTextBox();
	}
	
}
