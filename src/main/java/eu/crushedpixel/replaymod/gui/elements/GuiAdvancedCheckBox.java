package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.gui.elements.listeners.CheckBoxListener;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.config.GuiCheckBox;

import java.util.ArrayList;
import java.util.List;

public class GuiAdvancedCheckBox extends GuiCheckBox implements GuiElement {
    public GuiAdvancedCheckBox(int xPos, int yPos, String displayString, boolean isChecked) {
        super(0, xPos, yPos, displayString, isChecked);
    }

    public GuiAdvancedCheckBox(String displayString, boolean isChecked, boolean isEnabled) {
        this(0, 0, displayString, isChecked);
        this.setElementEnabled(isEnabled);
    }

    private List<CheckBoxListener> listeners = new ArrayList<CheckBoxListener>();

    public void addCheckBoxListener(CheckBoxListener listener) {
        this.listeners.add(listener);
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
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        boolean success = mousePressed(mc, mouseX, mouseY);
        if(success) fireCheckListeners();
        return success;
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

    @Override
    public void setIsChecked(boolean isChecked) {
        super.setIsChecked(isChecked);
        fireCheckListeners();
    }

    private void fireCheckListeners() {
        for(CheckBoxListener listener : listeners) {
            listener.onCheck(isChecked());
        }
    }
}
