package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.entity.StereoscopicEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class StereoscopicFrameRenderer extends FrameRenderer {
    private final StereoscopicEntityRenderer entityRenderer = new StereoscopicEntityRenderer();

    public StereoscopicFrameRenderer() {
        super(Minecraft.getMinecraft().displayWidth * 2, Minecraft.getMinecraft().displayHeight);
        setCustomEntityRenderer(entityRenderer);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage image = new BufferedImage(getVideoWidth(), getVideoHeight(), BufferedImage.TYPE_INT_RGB);
        try {
            captureFrame(timer, image, true);
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Rendering left eye.");
            throw new ReportedException(crash);
        }
        try {
            captureFrame(timer, image, false);
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Rendering right eye.");
            throw new ReportedException(crash);
        }
        return image;
    }

    protected void captureFrame(Timer timer, BufferedImage into, boolean leftEye) {
        int displayWidth = getVideoWidth() / 2;

        // Render frame
        entityRenderer.setEye(leftEye);
        renderFrame(timer);

        // Read pixels
        readPixels(buffer);

        // Copy to image
        copyPixelsToImage(buffer, displayWidth, into, leftEye ? 0 : displayWidth, 0);
        updateDefaultPreview(into);
    }
}
