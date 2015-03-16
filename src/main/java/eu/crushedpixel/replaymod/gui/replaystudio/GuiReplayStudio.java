package eu.crushedpixel.replaymod.gui.replaystudio;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

import org.apache.commons.io.FilenameUtils;

import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

public class GuiReplayStudio extends GuiScreen {

	public static GuiReplayStudio instance = null;

	private static final int tabYPos = 110;

	private enum StudioTab {
		TRIM(new GuiTrimPart(tabYPos)), CONNECT(new GuiConnectPart(tabYPos)), MODIFY(new GuiConnectPart(tabYPos));

		private GuiStudioPart studioPart;

		public GuiStudioPart getStudioPart() {
			return studioPart;
		}

		private StudioTab(GuiStudioPart part) {
			this.studioPart = part;
		}
	}

	public GuiReplayStudio() {
		instance = this;
	}

	private StudioTab currentTab = StudioTab.TRIM;

	private GuiDropdown replayDropdown;
	private GuiButton saveModeButton, saveButton;

	private boolean overrideSave = false;

	private boolean initialized = false;

	private List<File> replayFiles = new ArrayList<File>();

	public File getSelectedFile() {
		try {
			return replayFiles.get(replayDropdown.getSelectionIndex());
		} catch(ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

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

		int modeWidth = tabButtons.get(0).width;

		if(!initialized) {
			replayDropdown = new GuiDropdown(1, fontRendererObj, 15+2+1+80, 60, this.width-30-8-80-modeWidth-4, 5);
			refreshReplayDropdown();
		} else {
			replayDropdown.width = this.width-30-8-80-modeWidth-4;
		}

		if(!initialized) {
			saveModeButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVEMODE_BUTTON, width-15-modeWidth-3, 60, getSaveModeLabel());
		} else {
			saveModeButton.xPosition = width-15-modeWidth-3;
		}
		saveModeButton.width = modeWidth;
		buttonList.add(saveModeButton);


		GuiButton backButton = new GuiButton(GuiConstants.REPLAY_EDITOR_BACK_BUTTON, width-70-18, height-20-5, "Back");
		backButton.width = 70;
		buttonList.add(backButton);

		saveButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVE_BUTTON, width-70-18, height-(2*20)-5-3, "Save");
		saveButton.width = 70;
		buttonList.add(saveButton);

		for(StudioTab tab : StudioTab.values()) {
			tab.getStudioPart().initGui();
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
		} else if(button.id == GuiConstants.REPLAY_EDITOR_BACK_BUTTON) {
			mc.displayGuiScreen(new GuiMainMenu());
		} else if(button.id == GuiConstants.REPLAY_EDITOR_TRIM_TAB) {
			currentTab = StudioTab.TRIM;
		} else if(button.id == GuiConstants.REPLAY_EDITOR_CONNECT_TAB) {
			currentTab = StudioTab.CONNECT;
		} else if(button.id == GuiConstants.REPLAY_EDITOR_MODIFY_TAB) {
			currentTab = StudioTab.MODIFY;
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
			throws IOException {
		replayDropdown.mouseClicked(mouseX, mouseY, mouseButton);
		currentTab.getStudioPart().mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		currentTab.getStudioPart().drawScreen(mouseX, mouseY, partialTicks);

		drawCenteredString(fontRendererObj, "§n"+currentTab.getStudioPart().getTitle(), width/2, 92, Color.WHITE.getRGB());

		List<String> rows = new ArrayList<String>();
		String remaining = currentTab.getStudioPart().getDescription();
		while(remaining.length() > 0) {
			String[] split = remaining.split(" ");
			String b = "";
			for(String sp : split) {
				b += sp+" ";
				if(fontRendererObj.getStringWidth(b.trim()) > width-30-70-20) {
					b = b.substring(0, b.trim().length()-(sp.length()));
					break;
				}
			}
			String trimmed = b.trim();
			rows.add(trimmed);
			try {
				remaining = remaining.substring(trimmed.length()+1);
			} catch(Exception e) {break;}
		}

		int i=0;
		for(String row : rows) {
			drawString(fontRendererObj, row, 30, height-(15*(rows.size()-i)), Color.WHITE.getRGB());
			i++;
		}

		drawCenteredString(fontRendererObj, "Replay Studio", this.width / 2, 10, 16777215);
		drawString(fontRendererObj, "Replay File:", 30, 67, Color.WHITE.getRGB());
		
		replayDropdown.drawTextBox();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		currentTab.getStudioPart().keyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void updateScreen() {
		currentTab.getStudioPart().updateScreen();
		super.updateScreen();
	}
}
