package eu.crushedpixel.replaymod.utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

public class BufferedImageUtils {

    public static int hashCode(BufferedImage bi) {
        byte[] data = ((DataBufferByte) bi.getData().getDataBuffer()).getData();
        return Arrays.hashCode(data);
    }
}
