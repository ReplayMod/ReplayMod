package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class MouseUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Point getMousePos() {
        Point scaled = getScaledDimensions();
        int width = (int) scaled.getX();
        int height = (int) scaled.getY();

        final int mouseX = (Mouse.getX() * width / mc.displayWidth);
        final int mouseY = (height - Mouse.getY() * height / mc.displayHeight);

        return new Point(mouseX, mouseY);
    }

    public static Point getScaledDimensions() {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        final int width = sr.getScaledWidth();
        final int heigth = sr.getScaledHeight();

        return new Point(width, heigth);
    }
}
