package eu.crushedpixel.replaymod.gui.elements;

import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.util.*;

public class ComposedElement implements GuiElement {

    private static final ComposedElementComparator COMPOSED_ELEMENT_COMPARATOR = new ComposedElementComparator();

    @Getter
    private List<GuiElement> parts = new ArrayList<GuiElement>();

    public ComposedElement(GuiElement...parts) {
        this.parts = new ArrayList<GuiElement>(Arrays.asList(parts));
        Collections.sort(this.parts, COMPOSED_ELEMENT_COMPARATOR);
    }

    public void addPart(GuiElement part) {
        parts.add(part);
        Collections.sort(parts, COMPOSED_ELEMENT_COMPARATOR);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovered) {
        draw(mc, mouseX, mouseY);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        GuiElement hovered = null;

        for (int i=parts.size()-1; i>=0; i--) {
            GuiElement part = parts.get(i);
            if(part.isHovering(mouseX, mouseY)) {
                hovered = part;
                break;
            }
        }

        for(GuiElement part : parts) {
            part.draw(mc, mouseX, mouseY, hovered == part);
        }
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        for (GuiElement part : parts) {
            part.drawOverlay(mc, mouseX, mouseY);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        for (GuiElement part : parts) {
            if (part.isHovering(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        boolean clicked = false;
        //iterate over elements in reverse order to first handle mouse clicks of elements that are drawn on top
        for (int i=0; i<parts.size(); i++) {
            GuiElement part = parts.get(parts.size() - 1 - i);

            //if GuiOutsideClickableElement, forward mouse clicks outside of that element
            if(!clicked || (part instanceof GuiOutsideClickableElement && !part.isHovering(mouseX, mouseY))) {
                boolean cl = part.mouseClick(mc, mouseX, mouseY, button);
                if(cl) clicked = cl;
            }
        }
        return clicked;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        for (GuiElement part : parts) {
            part.mouseDrag(mc, mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        for (GuiElement part : parts) {
            part.mouseRelease(mc, mouseX, mouseY, button);
        }
    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {
        for (GuiElement part : parts) {
            part.buttonPressed(mc, mouseX, mouseY, key, keyCode);
        }
    }

    @Override
    public void tick(Minecraft mc) {
        for (GuiElement part : parts) {
            part.tick(mc);
        }
    }

    @Override
    public void setElementEnabled(boolean enabled) {
        for(GuiElement part : parts) {
            part.setElementEnabled(enabled);
        }
    }

    private static class ComposedElementComparator implements Comparator<GuiElement> {

        @Override
        public int compare(GuiElement o1, GuiElement o2) {
            Boolean d1 = o1 instanceof GuiOverlayElement;
            Boolean d2 = o2 instanceof GuiOverlayElement;

            if(d1 && d2) {
                return -new Integer(o1.yPos()).compareTo(o2.yPos());
            }

            return d1.compareTo(d2);
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    @Override
    public int xPos() {
        return 0;
    }

    @Override
    public int yPos() {
        return 0;
    }

    @Override
    public int width() {
        return 0;
    }

    @Override
    public int height() {
        return 0;
    }
}
