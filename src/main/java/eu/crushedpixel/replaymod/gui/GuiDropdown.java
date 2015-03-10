package eu.crushedpixel.replaymod.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiDropdown extends GuiTextField {

	private int selectionIndex = 0;
	private boolean open = false;

	private Minecraft mc = Minecraft.getMinecraft();

	private final int visibleDropout = 5;
	private final int dropoutElementHeight = 14;
	private final int maxDropoutHeight = dropoutElementHeight*visibleDropout;

	private int upperIndex = 0;

	public GuiDropdown(int id, FontRenderer fontRenderer,
			int xPos, int yPos, int width) {
		super(id, fontRenderer, xPos, yPos, width, 20);
		setCursorPositionZero();
	}
	
	@Override
	public void drawTextBox() {
		setCursorPositionZero();
		if(elements.size() > selectionIndex) {
			setText(mc.fontRendererObj.trimStringToWidth(
					elements.get(selectionIndex).toString(), width-8));
		} else {
			setText("");
		}
		super.drawTextBox();

		//Draw the right part of the Dropdown
		drawRect(xPosition+width-height, yPosition, xPosition+width, yPosition+height, -16777216);
		drawRect(xPosition+width-height, yPosition, this.xPosition+width-height+1, yPosition+height, -6250336);

		//heroically draw the triangle line by line instead of using a texture
		for(int i=0; i<=Math.ceil(height/2)-4; i++) {
			drawHorizontalLine(xPosition+width-height+i+4, xPosition+width-i-4, yPosition+(height/4)+i+2, -6250336);
		}

		if(open && elements.size() > 0) {
			//draw the dropout part when opened

			boolean drawScrollBar = false;

			int requiredHeight = elements.size()*dropoutElementHeight;
			if(requiredHeight > maxDropoutHeight) {
				requiredHeight = maxDropoutHeight;
				drawScrollBar = true;
			}

			//The light outline
			drawRect(xPosition-1, yPosition+height, xPosition+width+1, yPosition+height+requiredHeight+1, -6250336);

			//The dark inside
			drawRect(xPosition, yPosition+height+1, xPosition+width, yPosition+height+requiredHeight, -16777216);

			//The elements
			int y = 0;
			int i = 0;
			for(Object obj : elements) {
				if(i<upperIndex) {
					i++;
					continue;
				}
				drawHorizontalLine(xPosition, xPosition+width, yPosition+height+y, -6250336);
				String toWrite = mc.fontRendererObj.trimStringToWidth(obj.toString(), width-8);
				drawString(mc.fontRendererObj, toWrite, xPosition+4, yPosition+height+y+4, Color.WHITE.getRGB());

				y += dropoutElementHeight;
				i++;
				if(y >= requiredHeight) {
					break;
				}
			}

			if(drawScrollBar) {
				//The scroll bar
				int dw = Mouse.getDWheel();
				if(dw > 0) {
					dw = -1;
				} else if(dw < 0) {
					dw = 1;
				}

				upperIndex = Math.max(Math.min(upperIndex+dw, elements.size()-visibleDropout), 0);

				drawRect(xPosition+width-3, yPosition+height+1, xPosition+width, yPosition+height+requiredHeight, Color.DARK_GRAY.getRGB());

				float visiblePerc = ((float)visibleDropout)/elements.size();
				int barHeight = (int)(visiblePerc*(requiredHeight-1));

				float posPerc = ((float)upperIndex)/elements.size();
				int barY = (int)(posPerc*(requiredHeight-1));

				drawRect(xPosition+width-3, yPosition+height+1+barY, xPosition+width, yPosition+height+2+barY+barHeight, -6250336);
			}
		}
	}

	@Override
	public void mouseClicked(int xPos, int yPos, int mouseButton) {
		if(xPos > xPosition+width-height && xPos < xPosition+width && yPos > yPosition && yPos < yPosition+height) {
			open = !open;
		} else {
			if(xPos > xPosition && xPos < xPosition+width && open) {
				int requiredHeight = Math.min(maxDropoutHeight, elements.size()*dropoutElementHeight);
				if(yPos > yPosition+height && yPos < yPosition+height+requiredHeight) {
					int clickedIndex = (int)Math.floor((yPos - (yPosition+height)) / dropoutElementHeight) + upperIndex;
					this.selectionIndex = clickedIndex;
				}
				open = false;
			} else {
				open = false;
			}
		}
	}

	@Override
	public void setText(String text) {
		if(!getText().equals(text)) {
			super.setText(text);
		}
	}

	private List<Object> elements = new ArrayList<Object>();

	private void select(int index) {
		this.selectionIndex = index;
	}

	public void setElements(List<Object> elements) {
		this.elements = elements;
	}

	public void clearElements() {
		this.elements = new ArrayList<Object>();
	}

	public void addElement(Object element) {
		this.elements.add(element);
	}

	public Object getElement(int index) {
		return elements.get(index);
	}

	public int getSelectionIndex() {
		return selectionIndex;
	}

}
