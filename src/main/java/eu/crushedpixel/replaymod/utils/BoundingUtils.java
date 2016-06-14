package eu.crushedpixel.replaymod.utils;

import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

public class BoundingUtils {

    public static Dimension fitIntoBounds(ReadableDimension toFit, ReadableDimension bounds) {
        int width = toFit.getWidth();
        int height = toFit.getHeight();

        float w = (float)width/bounds.getWidth();
        float h = (float)height/bounds.getHeight();

        if(w > h) {
            height = (int)(height/w);
            width = (int)(width/w);
        } else {
            height = (int)(height/h);
            width = (int)(width/h);
        }

        return new Dimension(width, height);
    }

}
