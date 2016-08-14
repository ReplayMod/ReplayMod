package eu.crushedpixel.replaymod.gui.elements;

import com.replaymod.core.utils.OpenGLUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import static com.replaymod.core.ReplayMod.TEXTURE;

public class GuiArrowButton extends GuiAdvancedButton {

    private static final int TEXTURE_X = 40;
    private static final int TEXTURE_Y = 80;
    private static final int TEXTURE_WIDTH = 12;
    private static final int TEXTURE_HEIGHT = 12;


    @AllArgsConstructor
    public enum Direction {
        UP(-90), DOWN(90), RIGHT(0), LEFT(180);

        @Getter
        private int rotation;
    }

    private Direction dir;

    public GuiArrowButton(int buttonId, int x, int y, String buttonText, Direction dir) {
        super(buttonId, x, y, buttonText);

        this.dir = dir;
        width = 20;
        height = 20;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        draw(mc, mouseX, mouseY);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovering) {
        try {
            super.draw(mc, mouseX, mouseY, hovering);

            mc.getTextureManager().bindTexture(TEXTURE);

            OpenGLUtils.drawRotatedRectWithCustomSizedTexture(xPosition+4, yPosition+4, dir.getRotation(),
                    TEXTURE_X, TEXTURE_Y, TEXTURE_WIDTH, TEXTURE_HEIGHT, 128, 128);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
