package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;

public abstract class DelegatingElement implements GuiElement {

    protected boolean enabled = true;

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
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovered) {
        delegate().draw(mc, mouseX, mouseY, hovered);
    }

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
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        return delegate().mouseClick(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        delegate().mouseDrag(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        delegate().mouseRelease(mc, mouseX, mouseY, button);
    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {
        delegate().buttonPressed(mc, mouseX, mouseY, key, keyCode);
    }

    @Override
    public void tick(Minecraft mc) {
        delegate().tick(mc);
    }

    @Override
    public void setElementEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int xPos() {
        return delegate().xPos();
    }

    @Override
    public int yPos() {
        return delegate().yPos();
    }

    @Override
    public int width() {
        return delegate().width();
    }

    @Override
    public int height() {
        return delegate().height();
    }

    @Override
    public void xPos(int x) {
        delegate().xPos(x);
    }

    @Override
    public void yPos(int y) {
        delegate().yPos(y);
    }

    @Override
    public void width(int width) {
        delegate().width(width);
    }

    @Override
    public void height(int height) {
        delegate().height(height);
    }
}
