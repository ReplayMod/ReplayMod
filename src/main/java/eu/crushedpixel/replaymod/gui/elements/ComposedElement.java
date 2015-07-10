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
    }

    public void addPart(GuiElement part) {
        parts.add(part);
        Collections.sort(parts, COMPOSED_ELEMENT_COMPARATOR);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        for (GuiElement part : parts) {
            part.draw(mc, mouseX, mouseY);
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
            clicked = parts.get(parts.size()-1-i).mouseClick(mc, mouseX, mouseY, button);
            if(clicked) break;
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
    public void setEnabled(boolean enabled) {
        for(GuiElement part : parts) {
            part.setEnabled(enabled);
        }
    }

    private static class ComposedElementComparator implements Comparator<GuiElement> {

        @Override
        public int compare(GuiElement o1, GuiElement o2) {
            Boolean d1 = o1 instanceof GuiDropdown;
            Boolean d2 = o2 instanceof GuiDropdown;

            return d1.compareTo(d2);
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
