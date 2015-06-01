package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class DefaultFrameRenderer extends FrameRenderer {

    public DefaultFrameRenderer(RenderOptions options) {
        super(options, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        renderFrame(timer);

        int width = getVideoWidth();
        int height = getVideoHeight();

        readPixels(buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        copyPixelsToImage(buffer, width, image, 0, 0);
        updateDefaultPreview(image);
        return image;
    }
}
