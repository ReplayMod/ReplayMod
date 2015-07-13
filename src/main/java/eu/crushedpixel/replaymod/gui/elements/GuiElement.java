package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;

public interface GuiElement {

    void draw(Minecraft mc, int mouseX, int mouseY);
    void drawOverlay(Minecraft mc, int mouseX, int mouseY);

    boolean isHovering(int mouseX, int mouseY);

    boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button);
    void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button);
    void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button);

    void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode);

    void tick(Minecraft mc);

    void setElementEnabled(boolean enabled);

}
