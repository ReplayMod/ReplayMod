package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;

public interface GuiElement {

    void draw(Minecraft mc, int mouseX, int mouseY);
    void drawOverlay(Minecraft mc, int mouseX, int mouseY);

    boolean isHovering(int mouseX, int mouseY);

    void mouseClick(Minecraft mc, int mouseX, int mouseY, int button);
    void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button);
    void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button);

}
