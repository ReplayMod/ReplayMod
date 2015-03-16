package eu.crushedpixel.replaymod.gui.elements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiEntryList<T> extends GuiTextField {

	private int selectionIndex = -1;

	private Minecraft mc = Minecraft.getMinecraft();

	private int visibleElements;
	public final static int elementHeight = 14;

	private int upperIndex = 0;

	private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

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
		try {
			super.drawTextBox();
			//drawing the entries
			for(int i=0; i-upperIndex<visibleElements; i++) {
				if(i<upperIndex) continue;
				
				if(i >= elements.size()) break;

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
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void mouseClicked(int xPos, int yPos, int mouseButton) {
		int clickedIndex = (int)Math.floor((yPos-yPosition) / elementHeight) + upperIndex;
		if(clickedIndex < elements.size() && clickedIndex >= 0) {
			selectionIndex = clickedIndex;
			fireSelectionChangeEvent();
		}
	}

	private void fireSelectionChangeEvent() {
		for(SelectionListener listener : selectionListeners) {
			listener.onSelectionChanged(selectionIndex);
		}
	}
	
	@Override
	public void setText(String text) {}

	private List<T> elements = new ArrayList<T>();

	public void setElements(List<T> elements) {
		this.elements = elements;
		if(selectionIndex == -1 && elements.size() > 0) {
			selectionIndex = 0;
		}
	}

	public void clearElements() {
		this.elements = new ArrayList<T>();
		selectionIndex = -1;
	}

	public void addElement(T element) {
		this.elements.add(element);
		if(selectionIndex == -1) {
			selectionIndex = 0;
		}
	}

	public T getElement(int index) {
		return elements.get(index);
	}

	public int getSelectionIndex() {
		return selectionIndex;
	}
	
	public void setSelectionIndex(int index) {
		this.selectionIndex = index;
		if(selectionIndex < 0) selectionIndex = -1;
		fireSelectionChangeEvent();
	}

	public void addSelectionListener(SelectionListener listener) {
		this.selectionListeners.add(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		this.selectionListeners.remove(listener);
	}

}
