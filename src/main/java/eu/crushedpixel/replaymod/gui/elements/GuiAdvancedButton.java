package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiAdvancedButton extends GuiButton implements GuiElement {

    public GuiAdvancedButton(int x, int y, String buttonText) {
        this(0, x, y, buttonText);
    }

    public GuiAdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText) {
        this(0, x, y, widthIn, heightIn, buttonText);
    }

    public GuiAdvancedButton(int id, int x, int y, String buttonText) {
        super(id, x, y, buttonText);
    }

    public GuiAdvancedButton(int id, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(id, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        drawButton(mc, mouseX, mouseY);
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {

    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        mousePressed(mc, mouseX, mouseY);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {

    }

    @Override
    public void tick(Minecraft mc) {

    }
}
