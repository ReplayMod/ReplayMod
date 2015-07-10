package eu.crushedpixel.replaymod.gui.elements;

import com.google.common.base.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.awt.*;

public class GuiAdvancedTextField extends GuiTextField implements GuiElement {
    public String hint;
    public int hintTextColor = Color.DARK_GRAY.getRGB();

    protected boolean isEnabled = true;
    private int disabledTextColor;

    public GuiAdvancedTextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(0, fontRenderer, x, y, width, height);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        if(!isEnabled) setFocused(false);
        super.setEnabled(isEnabled);
    }

    @Override
    public void setDisabledTextColour(int disabledTextColor) {
        this.disabledTextColor = disabledTextColor;
        super.setDisabledTextColour(disabledTextColor);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        drawTextBox();
    }

    @Override
    public void drawTextBox() {
        if (text.isEmpty() && !isFocused() && !Strings.isNullOrEmpty(hint)) {
            super.setEnabled(false);
            super.setDisabledTextColour(hintTextColor);
            text = hint;

            super.drawTextBox();

            text = "";
            super.setDisabledTextColour(disabledTextColor);
            super.setEnabled(isEnabled);
        } else {
            super.drawTextBox();
        }
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
        mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {
        textboxKeyTyped(key, keyCode);
    }

    @Override
    public void tick(Minecraft mc) {
        updateCursorCounter();
    }

    @Override
    public void setFocused(boolean focused) {
        if(!isEnabled) focused = false;
        super.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return isEnabled ? super.isFocused() : false;
    }
}
