package eu.crushedpixel.replaymod.gui.replaystudio;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.apache.commons.io.FilenameUtils;

import de.johni0702.replaystudio.studio.ReplayStudio;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.GuiDropdown;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

public class GuiReplayStudio extends GuiScreen {

	private GuiDropdown replayDropdown;
	private GuiButton saveModeButton, saveButton;

	private boolean overrideSave = false;

	private boolean initialized = false;

	private List<File> replayFiles = new ArrayList<File>();
	
	private GuiTrimPart trimPart = new GuiTrimPart(100);

	private void refreshReplayDropdown() {
		replayDropdown.clearElements();
		replayFiles = ReplayFileIO.getAllReplayFiles();
		for(File file : replayFiles) {
			String name = FilenameUtils.getBaseName(file.getAbsolutePath());
			replayDropdown.addElement(name);
		}
	}

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

		if(!initialized) {
			replayDropdown = new GuiDropdown(1, fontRendererObj, 15+2+1+100, 60, this.width-30-8-100);
			refreshReplayDropdown();
		} else {
			replayDropdown.width = this.width-30-8-100;
		}

		List<GuiButton> saveButtons = new ArrayList<GuiButton>();
		
		if(!initialized) {
			saveButtons.add(saveModeButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVEMODE_BUTTON, 0, 0, getSaveModeLabel()));
			saveButtons.add(saveButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVE_BUTTON, 0, 0, "Save Replay"));
		} else {
			saveButtons.add(saveModeButton);
			saveButtons.add(saveButton);
		}
		
		w = this.width-30-100;
		w2 = w/saveButtons.size();
		i = 0;
		for(GuiButton b : saveButtons) {
			int x = 15+100+(w2*i);
			b.xPosition = x+2;
			b.yPosition = 90;
			b.width = w2-4;
			
			buttonList.add(b);
			
			i++;
		}
		
		initialized = true;
	};
	
	private String getSaveModeLabel() {
		return overrideSave ? "Replace Source File" : "Save to new File";
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(!button.enabled) return;
		if(button.id == GuiConstants.REPLAY_EDITOR_SAVEMODE_BUTTON) {
			overrideSave = !overrideSave;
			button.displayString = getSaveModeLabel();
		}
	}
	
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
		this.drawString(fontRendererObj, "Replay File:", 30, 67, Color.WHITE.getRGB());
		this.drawString(fontRendererObj, "Save Mode:", 30, 97, Color.WHITE.getRGB());
		super.drawScreen(mouseX, mouseY, partialTicks);
		replayDropdown.drawTextBox();
		trimPart.drawScreen(mouseX, mouseY, partialTicks);
		
	}
}
