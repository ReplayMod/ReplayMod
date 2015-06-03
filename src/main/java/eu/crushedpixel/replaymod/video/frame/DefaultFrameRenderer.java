package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class DefaultFrameRenderer extends FrameRenderer {

    public DefaultFrameRenderer(RenderOptions options) {
        super(options);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        int width = getVideoWidth();
        int height = getVideoHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        renderFrame(timer, image, 0, 0);
        updateDefaultPreview(image);
        return image;
    }
}
