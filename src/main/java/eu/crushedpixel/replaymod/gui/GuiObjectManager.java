package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.gui.elements.ComposedElement;
import eu.crushedpixel.replaymod.gui.elements.GuiDraggingNumberInput;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.util.Point;

import java.io.IOException;

public class GuiObjectManager extends GuiScreen {

    private boolean initialized = false;

    private GuiDraggingNumberInput anchorXInput, anchorYInput, anchorZInput;

    private ComposedElement numberInputs;

    @Override
    public void initGui() {
        if(!initialized) {
            anchorXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            anchorYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            anchorZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);

            numberInputs = new ComposedElement(anchorXInput, anchorYInput, anchorZInput);
        }

        anchorXInput.width = anchorYInput.width = anchorZInput.width = 45;
        anchorXInput.yPosition = anchorYInput.yPosition = anchorZInput.yPosition = 50;

        anchorXInput.xPosition = 20;
        anchorYInput.xPosition = anchorXInput.xPosition+anchorXInput.width + 5;
        anchorZInput.xPosition = anchorYInput.xPosition+anchorYInput.width + 5;

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        numberInputs.draw(mc, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Point mousePos = MouseUtils.getMousePos();
        numberInputs.buttonPressed(mc, mousePos.getX(), mousePos.getY(), typedChar, keyCode);

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        numberInputs.tick(mc);
        super.updateScreen();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        numberInputs.mouseClick(mc, mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        numberInputs.mouseDrag(mc, mouseX, mouseY, mouseButton);
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        numberInputs.mouseRelease(mc, mouseX, mouseY, mouseButton);
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }
}
