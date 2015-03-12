package eu.crushedpixel.replaymod.gui.replaystudio;

import net.minecraft.client.Minecraft;

import org.apache.commons.io.FilenameUtils;

import eu.crushedpixel.replaymod.gui.GuiEntryList;

public class GuiConnectPart extends GuiStudioPart {

	private Minecraft mc = Minecraft.getMinecraft();

	private static final String DESCRIPTION = "Connects multiple Replays in the same order as the list.";
	private static final String TITLE = "Connect Replays";

	private boolean initialized = false;
	
	private GuiEntryList concatList;

	public GuiConnectPart(int yPos) {
		super(yPos);
		fontRendererObj = mc.fontRendererObj;
	}

	@Override
	public void applyFilters() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getTitle() {
		return TITLE;
	}

	@Override
	public void initGui() {
		if(!initialized) {
			concatList = new GuiEntryList(1, fontRendererObj, 30, yPos, 200, 0);
			concatList.addElement(FilenameUtils.getBaseName(GuiReplayStudio.instance.getSelectedFile().getAbsolutePath()));
		
			concatList.addElement("test1");
			concatList.addElement("test2");
			concatList.addElement("test3");
			concatList.addElement("test4");
			concatList.addElement("test5");
			concatList.addElement("test6");
			concatList.addElement("test7");
			concatList.addElement("test8");
			concatList.addElement("test9");
			concatList.addElement("test10");
			concatList.addElement("test11");
		}
		int h = GuiReplayStudio.instance.height-yPos-20;
		int rows = (int)(h / (float)GuiEntryList.elementHeight);
		concatList.setVisibleElements(rows);
		
		initialized = true;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		concatList.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		concatList.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen() {
		if(!initialized) initGui();
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {

	}
}
