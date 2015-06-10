package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuiDropdown<T> extends GuiTextField {

    private final int visibleDropout;
    private final int dropoutElementHeight = 14;
    private final int maxDropoutHeight;
    private int selectionIndex = -1;
    private boolean open = false;
    private Minecraft mc = Minecraft.getMinecraft();
    private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

    private int upperIndex = 0;
    private List<T> elements = new ArrayList<T>();
    private HashMap<Integer, String> hoverTexts = new HashMap<Integer, String>();

    public GuiDropdown(int id, FontRenderer fontRenderer,
                       int xPos, int yPos, int width, int visibleDropout) {
        super(id, fontRenderer, xPos, yPos, width, 20);
        this.visibleDropout = visibleDropout;
        this.maxDropoutHeight = dropoutElementHeight * visibleDropout;
    }

    @Override
    public void drawTextBox() {
        if(elements.size() > selectionIndex && selectionIndex >= 0) {
            setText(mc.fontRendererObj.trimStringToWidth(
                    elements.get(selectionIndex).toString(), width - 8));
        } else {
            setText("");
        }
        super.drawTextBox();

        //Draw the right part of the Dropdown
        drawRect(xPosition + width - height, yPosition, xPosition + width, yPosition + height, -16777216);
        drawRect(xPosition + width - height, yPosition, this.xPosition + width - height + 1, yPosition + height, -6250336);

        //heroically draw the triangle line by line instead of using a texture
        for(int i = 0; i <= Math.ceil(height / 2) - 4; i++) {
            drawHorizontalLine(xPosition + width - height + i + 4, xPosition + width - i - 4, yPosition + (height / 4) + i + 2, -6250336);
        }

        boolean drawHover = false;
        Point hoverPos = null;
        String hoverText = null;

        if(open && elements.size() > 0) {
            //draw the dropout part when opened

            boolean drawScrollBar = false;

            int requiredHeight = elements.size() * dropoutElementHeight;
            if(requiredHeight > maxDropoutHeight) {
                requiredHeight = maxDropoutHeight;
                drawScrollBar = true;
            }

            //The light outline
            drawRect(xPosition - 1, yPosition + height, xPosition + width + 1, yPosition + height + requiredHeight + 1, -6250336);

            //The dark inside
            drawRect(xPosition, yPosition + height + 1, xPosition + width, yPosition + height + requiredHeight, -16777216);

            //The elements
            int y = 0;
            int i = 0;
            for(T obj : elements) {
                if(i < upperIndex) {
                    i++;
                    continue;
                }
                drawHorizontalLine(xPosition, xPosition + width, yPosition + height + y, -6250336);
                String toWrite = mc.fontRendererObj.trimStringToWidth(obj.toString(), width - 8);
                drawString(mc.fontRendererObj, toWrite, xPosition + 4, yPosition + height + y + 4, Color.WHITE.getRGB());

                if(MouseUtils.isMouseWithinBounds(xPosition, yPosition + height + y, width, dropoutElementHeight)) {
                    String hover = hoverTexts.get(i);
                    if(hover != null) {
                        Point mousePos = MouseUtils.getMousePos();
                        drawHover = true;
                        hoverPos = mousePos;
                        hoverText = hover;
                    }
                }

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

                upperIndex = Math.max(Math.min(upperIndex + dw, elements.size() - visibleDropout), 0);

                drawRect(xPosition + width - 3, yPosition + height + 1, xPosition + width, yPosition + height + requiredHeight, Color.DARK_GRAY.getRGB());

                float visiblePerc = ((float) visibleDropout) / elements.size();
                int barHeight = (int) (visiblePerc * (requiredHeight - 1));

                float posPerc = ((float) upperIndex) / elements.size();
                int barY = (int) (posPerc * (requiredHeight - 1));

                drawRect(xPosition + width - 3, yPosition + height + barY, xPosition + width, yPosition + height + 2 + barY + barHeight, -6250336);
            }

            if(drawHover) {
                ReplayMod.tooltipRenderer.drawTooltip(hoverPos.getX(), hoverPos.getY(), hoverText, null, Color.WHITE);
            }
        }
    }

    @Override
    public void mouseClicked(int xPos, int yPos, int mouseButton) {
        mouseClickedResult(xPos, yPos, mouseButton);
    }


    public boolean mouseClickedResult(int xPos, int yPos, int mouseButton) {
        boolean success = false;
        if(xPos > xPosition + width - height && xPos < xPosition + width && yPos > yPosition && yPos < yPosition + height) {
            open = !open;
        } else {
            if(xPos > xPosition && xPos < xPosition + width && open) {
                int requiredHeight = Math.min(maxDropoutHeight, elements.size() * dropoutElementHeight);
                if(yPos > yPosition + height && yPos < yPosition + height + requiredHeight) {
                    int clickedIndex = (int) Math.floor((yPos - (yPosition + height)) / dropoutElementHeight) + upperIndex;
                    this.selectionIndex = clickedIndex;
                    success = true;
                    fireSelectionChangeEvent();
                }
                open = false;
            } else {
                open = false;
            }
        }
        return success;
    }

    @Override
    public void setText(String text) {
        if(!getText().equals(text)) {
            super.setText(text);
        }
    }

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

    public void setHoverText(int index, String text) {
        hoverTexts.put(index, text);
    }

    public T getElement(int index) {
        return elements.get(index);
    }

    public List<T> getAllElements() {
        return elements;
    }

    public int getSelectionIndex() {
        return selectionIndex;
    }

    public void setSelectionIndex(int index) {
        this.selectionIndex = index;
        if(selectionIndex < 0) selectionIndex = -1;
        fireSelectionChangeEvent();
    }

    private void fireSelectionChangeEvent() {
        for(SelectionListener listener : selectionListeners) {
            listener.onSelectionChanged(selectionIndex);
        }
    }

    public void addSelectionListener(SelectionListener listener) {
        this.selectionListeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        this.selectionListeners.remove(listener);
    }

    public boolean isExpanded() {
        return open;
    }
}
