package eu.crushedpixel.replaymod.video.entity.strategy;

import java.awt.image.BufferedImage;

public interface FrameRenderingStrategy {
    void renderFrame(float partialTicks, BufferedImage into, int x, int y);
    void cleanup();
}
