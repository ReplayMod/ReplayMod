package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiEntryList<T extends GuiEntryListEntry> extends GuiAdvancedTextField {

    public final static int elementHeight = 14;
    private int selectionIndex = -1;
    private int visibleElements;
    private int upperIndex = 0;

    private String emptyMessage = null;

    private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();
    private List<T> elements = new ArrayList<T>();

    public GuiEntryList(FontRenderer fontRenderer,
                        int xPos, int yPos, int width, int visibleEntries) {
        super(fontRenderer, xPos, yPos, width, elementHeight * visibleEntries - 1);
        this.visibleElements = visibleEntries;
    }

    public void setVisibleElements(int rows) {
        this.visibleElements = rows;
        this.height = elementHeight * visibleElements - 1;
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    @Override
    @Deprecated
    public void drawTextBox() {
        Point mousePos = MouseUtils.getMousePos();
        draw(Minecraft.getMinecraft(), mousePos.getX(), mousePos.getY());
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        try {
            super.drawTextBox();

            if(elements.size() == 0 && emptyMessage != null) {
                drawCenteredString(mc.fontRendererObj, emptyMessage,
                        xPosition+(width/2), yPosition+(height/2)-(mc.fontRendererObj.FONT_HEIGHT/2), Color.RED.getRGB());
            }

            //drawing the entries
            for(int i = 0; i - upperIndex < visibleElements; i++) {
                if(i < upperIndex) continue;

                if(i >= elements.size()) break;

                if(i == selectionIndex) {
                    drawRect(xPosition, yPosition + (i - upperIndex) * elementHeight, xPosition + width,
                            yPosition + (i + 1 - upperIndex) * elementHeight - 1, Color.GRAY.getRGB());
                }

                drawRect(xPosition, yPosition + (i + 1 - upperIndex) * elementHeight - 1, xPosition + width,
                        yPosition + (i + 1 - upperIndex) * elementHeight, -6250336);
                drawString(mc.fontRendererObj, mc.fontRendererObj.trimStringToWidth(elements.get(i).getDisplayString(), width - 4),
                        xPosition + 2, yPosition + (i - upperIndex) * elementHeight + 3, Color.WHITE.getRGB());
            }

            //drawing the scroll bar
            if(elements.size() > visibleElements) {
                //handle scroll events
                int dw = 0;

                if(isHovering(mouseX, mouseY)) {
                    dw = Mouse.getDWheel();
                    if(dw > 0) {
                        dw = -1;
                    } else if(dw < 0) {
                        dw = 1;
                    }
                }

                upperIndex = Math.max(Math.min(upperIndex + dw, elements.size() - visibleElements), 0);

                float visiblePerc = ((float) visibleElements) / elements.size();
                int barHeight = (int) (visiblePerc * (height - 1));

                float posPerc = ((float) upperIndex) / elements.size();
                int barY = (int) (posPerc * (height - 1));

                drawRect(xPosition + width - 3, yPosition, xPosition + width, yPosition + height, Color.DARK_GRAY.getRGB());
                drawRect(xPosition + width - 3, yPosition + barY, xPosition + width, yPosition + 1 + barY + barHeight, -6250336);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void mouseClicked(int xPos, int yPos, int mouseButton) {
        if(!(xPos >= xPosition && xPos <= xPosition + width && yPos >= yPosition && yPos <= yPosition + height)) return;
        int clickedIndex = (int) Math.floor((yPos - yPosition) / elementHeight) + upperIndex;
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
    public void setText(String text) {
    }

    public void setElements(List<T> elements) {
        this.elements = elements;
        if(selectionIndex == -1 && elements.size() > 0) {
            selectionIndex = 0;
        }
    }

    public void addElement(T element) {
        if(element == null) return;
        this.elements.add(element);
        selectionIndex = elements.size()-1;
        fireSelectionChangeEvent();
    }

    public T getElement(int index) {
        if(index >= 0) {
            return elements.get(index);
        }
        return null;
    }

    public void removeElement(int index) {
        elements.remove(index);
        if(selectionIndex >= elements.size()) {
            selectionIndex = elements.size()-1;
        }
        fireSelectionChangeEvent();
    }

    public int getSelectionIndex() {
        return selectionIndex;
    }

    public void setSelectionIndex(int index) {
        this.selectionIndex = index;
        if(selectionIndex < 0) selectionIndex = -1;
        fireSelectionChangeEvent();
    }

    public List<T> getCopyOfElements() {
        return new ArrayList<T>(elements);
    }
    public List<T> getElements() {
        return elements;
    }

    /*
    public void replaceElement(int index, T replace) {
        elements.set(index, replace);
        fireSelectionChangeEvent();
    }
    */

    public int getEntryCount() {
        return elements.size();
    }

    public void addSelectionListener(SelectionListener listener) {
        this.selectionListeners.add(listener);
    }

}
