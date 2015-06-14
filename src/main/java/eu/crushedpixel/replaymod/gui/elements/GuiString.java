package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.concurrent.Callable;

public class GuiString extends Gui implements GuiElement {
    public int positionX, positionY;
    public Color color;
    public Callable<String> getContent;

    public GuiString(int positionX, int positionY, Color color, final String content) {
        this(positionX, positionY, color, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return content;
            }
        });
    }

    public GuiString(int positionX, int positionY, Color color, Callable<String> getContent) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.color = color;
        this.getContent = getContent;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        String text;
        try {
            text = getContent.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        drawString(mc.fontRendererObj, text, positionX, positionY, color.getRGB());
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {

    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {

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
