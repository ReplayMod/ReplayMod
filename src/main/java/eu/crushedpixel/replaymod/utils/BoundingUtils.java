package eu.crushedpixel.replaymod.utils;

import org.lwjgl.util.Dimension;

public class BoundingUtils {

    public static Dimension fitIntoBounds(Dimension toFit, Dimension bounds) {
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
