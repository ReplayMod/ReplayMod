package eu.crushedpixel.replaymod.gui.replaystudio;

import java.awt.Color;
import java.io.IOException;

import eu.crushedpixel.replaymod.gui.GuiNumberInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class GuiTrimPart extends GuiStudioPart {

	private int yPos = 0;

	private Minecraft mc = Minecraft.getMinecraft();

	private static final String DESCRIPTION = "Removes the beginning and end of a Replay File and only keeps the Replay between the given Timestamps";
	private static final String TITLE = "Trim Replay";
	
	private boolean initialized = false;

	private GuiNumberInput startMinInput, startSecInput, startMsInput;
	private GuiNumberInput endMinInput, endSecInput, endMsInput;

	public GuiTrimPart(int yPos) {
		this.yPos = yPos;
		fontRendererObj = mc.fontRendererObj;
		initGui();
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
			startMinInput = new GuiNumberInput(1, fontRendererObj, 70, yPos, 30, 3);
			startSecInput = new GuiNumberInput(1, fontRendererObj, 120, yPos, 25, 2);
			startMsInput = new GuiNumberInput(1, fontRendererObj, 165, yPos, 30, 3);

			endMinInput = new GuiNumberInput(1, fontRendererObj, 70, yPos+30, 30, 3);
			endSecInput = new GuiNumberInput(1, fontRendererObj, 120, yPos+30, 25, 2);
			endMsInput = new GuiNumberInput(1, fontRendererObj, 165, yPos+30, 30, 3);
		}

		initialized = true;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		startMinInput.mouseClicked(mouseX, mouseY, mouseButton);
		startSecInput.mouseClicked(mouseX, mouseY, mouseButton);
		startMsInput.mouseClicked(mouseX, mouseY, mouseButton);
		endMinInput.mouseClicked(mouseX, mouseY, mouseButton);
		endSecInput.mouseClicked(mouseX, mouseY, mouseButton);
		endMsInput.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawString(mc.fontRendererObj, "Start:", 30, yPos+7, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "End:", 30, yPos+7+30, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "m", 105, yPos+7, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "m", 105, yPos+7+30, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "s", 150, yPos+7, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "s", 150, yPos+7+30, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "ms", 200, yPos+7, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "ms", 200, yPos+7+30, Color.WHITE.getRGB());
		
		startMinInput.drawTextBox();
		startSecInput.drawTextBox();
		startMsInput.drawTextBox();
		endMinInput.drawTextBox();
		endSecInput.drawTextBox();
		endMsInput.drawTextBox();
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen() {
		if(initialized) {
			startMinInput.updateCursorCounter();
			startSecInput.updateCursorCounter();
			startMsInput.updateCursorCounter();
			endMinInput.updateCursorCounter();
			endSecInput.updateCursorCounter();
			endMsInput.updateCursorCounter();
		}
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		startMinInput.textboxKeyTyped(typedChar, keyCode);
		startSecInput.textboxKeyTyped(typedChar, keyCode);
		startMsInput.textboxKeyTyped(typedChar, keyCode);
		endMinInput.textboxKeyTyped(typedChar, keyCode);
		endSecInput.textboxKeyTyped(typedChar, keyCode);
		endMsInput.textboxKeyTyped(typedChar, keyCode);
	}
}
