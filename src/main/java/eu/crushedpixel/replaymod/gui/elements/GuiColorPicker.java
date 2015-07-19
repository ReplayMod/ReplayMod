package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;

import java.awt.*;

public class GuiColorPicker extends GuiAdvancedButton {

    private final int PICKER_SIZE = 100;

    private boolean pickerVisible = false;
    private int pickedColor = Color.BLACK.getRGB();

    public int pickerX, pickerY;

    public GuiColorPicker(int buttonId, int x, int y, String text, int pickerX, int pickerY) {
        super(buttonId, x, y, text);
        this.pickerX = pickerX;
        this.pickerY = pickerY;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if(this.visible) {
            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int k = this.getHoverState(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.blendFunc(770, 771);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + k * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + k * 20, this.width / 2, this.height);
            this.mouseDragged(mc, mouseX, mouseY);
            int l = 14737632;

            if (packedFGColour != 0) {
                l = packedFGColour;
            }
            else if (!this.enabled) {
                l = 10526880;
            }
            else if (this.hovered) {
                l = 16777120;
            }

            int strWidth = fontrenderer.getStringWidth(this.displayString);

            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2 - 10, this.yPosition + (this.height - 8) / 2, l);
            this.drawGradientRect(this.xPosition + (width+strWidth)/2 - 6, this.yPosition + 4, this.xPosition + (width+strWidth)/2 + 6,
                    this.yPosition + 4 + 12, Color.BLACK.getRGB(), Color.BLACK.getRGB());
            this.drawGradientRect(this.xPosition + (width+strWidth)/2 - 5, this.yPosition + 5, this.xPosition + (width+strWidth)/2 + 5,
                    this.yPosition + 5 + 10, pickedColor, pickedColor);

            if(pickerVisible && this.enabled) {
                this.drawGradientRect(pickerX-1, pickerY-1, pickerX+PICKER_SIZE+1, pickerY+PICKER_SIZE+1, Color.BLACK.getRGB(), Color.BLACK.getRGB());
                for(int x=0; x < PICKER_SIZE; x++) {
                    for(int y=0; y < PICKER_SIZE; y++) {
                        int color = getColorAtPosition(x, y);
                        this.drawGradientRect(pickerX+x, pickerY+y, pickerX+x+1, pickerY+y+1, color, color);
                    }
                }
            }
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if(pickerVisible && this.enabled) {
            if(MouseUtils.isMouseWithinBounds(pickerX, pickerY, PICKER_SIZE, PICKER_SIZE)) {
                Point mousePoint = MouseUtils.getMousePos();
                setPickerColor(getColorAtPosition(mousePoint.getX() - pickerX, mousePoint.getY() - pickerY));
            }
        }
        return super.mousePressed(mc, mouseX, mouseY);
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        boolean clicked = super.mouseClick(mc, mouseX, mouseY, button);
        if(clicked) pickerToggled();
        return clicked;
    }

    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if(Mouse.isButtonDown(0)) mousePressed(mc, mouseX, mouseY);
    }

    public void pickerToggled() {
        pickerVisible = !pickerVisible;
    }

    private int getColorAtPosition(int x, int y) {
        if(x < 0 || x > PICKER_SIZE || y < 0 || y > PICKER_SIZE) return 0;

        boolean grey = x >= (PICKER_SIZE-5);

        float h = grey ? 0 : (float)x / (PICKER_SIZE - 5);
        float s = grey ? 0 : y <= (PICKER_SIZE/2) ? (float)y / (PICKER_SIZE/2) : 1;
        float v = grey ? 1 - ((float)y / PICKER_SIZE) : y > (PICKER_SIZE/2) ? 1 - ((float)(y-(PICKER_SIZE/2)) / (PICKER_SIZE/2f)) : 1;

        return Color.HSBtoRGB(h, s, v);
    }

    public void setPickerColor(int color) {
        this.pickedColor = color;
    }

    public int getPickedColor() {
        return pickedColor & 0xffffff;
    }
}
