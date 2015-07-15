package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import java.awt.*;

public class GuiAdvancedButton extends GuiButton implements GuiElement {
    private final Runnable action;
    public String hoverText;

    public GuiAdvancedButton(int id, int x, int y, String buttonText) {
        this(id, x, y, buttonText, null, null);
    }

    public GuiAdvancedButton(int x, int y, int width, int height, String buttonText, Runnable action, String hoverText) {
        this(0, x, y, width, height, buttonText, action, hoverText);
    }

    public GuiAdvancedButton(int id, int x, int y, String buttonText, Runnable action, String hoverText) {
        super(id, x, y, buttonText);
        this.action = action;
        this.hoverText = hoverText;
    }

    public GuiAdvancedButton(int id, int x, int y, int width, int height, String buttonText, Runnable action, String hoverText) {
        super(id, x, y, width, height, buttonText);
        this.action = action;
        this.hoverText = hoverText;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        drawButton(mc, mouseX, mouseY);
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        hovered = isHovering(mouseX, mouseY);
        if(hovered && hoverText != null) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, hoverText, null, Color.WHITE);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if (isHovering(mouseX, mouseY)) {
            performAction();
            return true;
        }
        return false;
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

    @Override
    public void setElementEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void performAction() {
        if (action != null) {
            action.run();
        }
    }
}
