package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;

public abstract class DelegatingElement implements GuiElement {
    public static DelegatingElement of(final GuiElement element) {
        return new DelegatingElement() {
            @Override
            public GuiElement delegate() {
                return element;
            }
        };
    }

    public abstract GuiElement delegate();

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        delegate().draw(mc, mouseX, mouseY);
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        delegate().drawOverlay(mc, mouseX, mouseY);
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return delegate().isHovering(mouseX, mouseY);
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        delegate().mouseClick(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        delegate().mouseDrag(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        delegate().mouseRelease(mc, mouseX, mouseY, button);
    }
}
