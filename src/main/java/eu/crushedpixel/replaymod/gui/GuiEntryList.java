package eu.crushedpixel.replaymod.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiEntryList extends GuiTextField {

	private int selectionIndex = -1;

	private Minecraft mc = Minecraft.getMinecraft();

	private int visibleElements;
	public final static int elementHeight = 14;

	private int upperIndex = 0;

	public GuiEntryList(int id, FontRenderer fontRenderer,
			int xPos, int yPos, int width, int visibleEntries) {
		super(id, fontRenderer, xPos, yPos, width, elementHeight*visibleEntries-1);
		this.visibleElements = visibleEntries;
	}

	public void setVisibleElements(int rows) {
		this.visibleElements = rows;
		this.height = elementHeight*visibleElements-1;
	}

	@Override
	public void drawTextBox() {
		super.drawTextBox();
		//drawing the entries
		for(int i=0; i-upperIndex<visibleElements; i++) {
			if(i<upperIndex) {
				continue;
			}
			
			if(i == selectionIndex) {
				drawRect(xPosition, yPosition+(i-upperIndex)*elementHeight, xPosition+width, 
						yPosition+(i+1-upperIndex)*elementHeight-1, Color.GRAY.getRGB());
			}
			
			drawRect(xPosition, yPosition+(i+1-upperIndex)*elementHeight-1, xPosition+width, 
					yPosition+(i+1-upperIndex)*elementHeight, -6250336);
			drawString(mc.fontRendererObj, mc.fontRendererObj.trimStringToWidth(elements.get(i).toString(), width-4), 
					xPosition+2, yPosition+(i-upperIndex)*elementHeight+3, Color.WHITE.getRGB());
		}

		//drawing the scroll bar
		if(elements.size() > visibleElements) {
			//handle scroll events
			int dw = Mouse.getDWheel();
			if(dw > 0) {
				dw = -1;
			} else if(dw < 0) {
				dw = 1;
			}

			upperIndex = Math.max(Math.min(upperIndex+dw, elements.size()-visibleElements), 0);

			float visiblePerc = ((float)visibleElements)/elements.size();
			int barHeight = (int)(visiblePerc*(height-1));

			float posPerc = ((float)upperIndex)/elements.size();
			int barY = (int)(posPerc*(height-1));

			drawRect(xPosition+width-3, yPosition, xPosition+width, yPosition+height, Color.DARK_GRAY.getRGB());
			drawRect(xPosition+width-3, yPosition+barY, xPosition+width, yPosition+1+barY+barHeight, -6250336);
		}
	}

	@Override
	public void mouseClicked(int xPos, int yPos, int mouseButton) {
		int clickedIndex = (int)Math.floor((yPos-yPosition) / elementHeight) + upperIndex;
		if(clickedIndex < elements.size()) selectionIndex = clickedIndex;
	}

	@Override
	public void setText(String text) {}

	private List<Object> elements = new ArrayList<Object>();

	private void select(int index) {
		this.selectionIndex = index;
		if(selectionIndex < 0) selectionIndex = -1;
	}

	public void setElements(List<Object> elements) {
		this.elements = elements;
		if(selectionIndex == -1 && elements.size() > 0) {
			selectionIndex = 0;
		}
	}

	public void clearElements() {
		this.elements = new ArrayList<Object>();
		selectionIndex = -1;
	}

	public void addElement(Object element) {
		this.elements.add(element);
		if(selectionIndex == -1) {
			selectionIndex = 0;
		}
	}

	public Object getElement(int index) {
		return elements.get(index);
	}

	public int getSelectionIndex() {
		return selectionIndex;
	}

}
