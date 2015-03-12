package eu.crushedpixel.replaymod.gui.replaystudio;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import eu.crushedpixel.replaymod.gui.GuiNumberInput;

public class GuiTrimPart extends GuiStudioPart {

	private Minecraft mc = Minecraft.getMinecraft();

	private static final String DESCRIPTION = "Removes the beginning and end of a Replay File and only keeps the Replay between the given timestamps.";
	private static final String TITLE = "Trim Replay";

	private boolean initialized = false;

	private GuiNumberInput startMinInput, startSecInput, startMsInput;
	private GuiNumberInput endMinInput, endSecInput, endMsInput;

	private List<GuiNumberInput> inputOrder = new ArrayList<GuiNumberInput>();

	public GuiTrimPart(int yPos) {
		super(yPos);
		fontRendererObj = mc.fontRendererObj;
	}

	@Override
	public void applyFilters() {
		// TODO Auto-generated method stub
	}

	private int valueOf(String text) {
		try {
			return Integer.valueOf(text);
		} catch(NumberFormatException e) {
			return 0;
		}
	}

	private int getStartTimestamp() {
		int mins = valueOf(startMinInput.getText());
		int secs = valueOf(startSecInput.getText());
		int ms = valueOf(startMsInput.getText());

		return (mins*60*1000)+(secs*1000)+ms;
	}

	private int getEndTimestamp() {
		int mins = valueOf(endMinInput.getText());
		int secs = valueOf(endSecInput.getText());
		int ms = valueOf(endMsInput.getText());

		return (mins*60*1000)+(secs*1000)+ms;
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

			inputOrder.clear();

			inputOrder.add(startMinInput);
			inputOrder.add(startSecInput);
			inputOrder.add(startMsInput);
			inputOrder.add(endMinInput);
			inputOrder.add(endSecInput);
			inputOrder.add(endMsInput);
		}

		initialized = true;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		for(GuiNumberInput input: inputOrder) {
			input.mouseClicked(mouseX, mouseY, mouseButton);
		}
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

		drawString(mc.fontRendererObj, "Timestamp: "+getStartTimestamp(), 230, yPos+7, Color.WHITE.getRGB());
		drawString(mc.fontRendererObj, "Timestamp: "+getEndTimestamp(), 230, yPos+30+7, Color.WHITE.getRGB());

		for(GuiNumberInput input: inputOrder) {
			input.drawTextBox();
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen() {
		if(!initialized) {
			initGui();
		} else {
			for(GuiNumberInput input: inputOrder) {
				input.updateCursorCounter();
			}
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(keyCode == Keyboard.KEY_TAB) { //Tab handling
			int i=0;
			for(GuiNumberInput input: inputOrder) {
				if(input.isFocused()) {
					input.setFocused(false);
					i++;
					if(i >= inputOrder.size()) i=0;
					inputOrder.get(i).setFocused(true);
					break;
				}
				i++;
			}
		} else {
			for(GuiNumberInput input: inputOrder) {
				input.textboxKeyTyped(typedChar, keyCode);
			}
		}
	}
}
