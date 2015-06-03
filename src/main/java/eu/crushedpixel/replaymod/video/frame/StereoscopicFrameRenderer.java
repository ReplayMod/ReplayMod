package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.entity.StereoscopicEntityRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class StereoscopicFrameRenderer extends FrameRenderer {
    private final StereoscopicEntityRenderer entityRenderer;

    public StereoscopicFrameRenderer(RenderOptions options) {
        super(options);
        entityRenderer = new StereoscopicEntityRenderer(options);
        setCustomEntityRenderer(entityRenderer);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage image = new BufferedImage(getVideoWidth(), getVideoHeight(), BufferedImage.TYPE_INT_RGB);
        try {
            entityRenderer.setEye(true);
            renderFrame(timer, image, 0, 0);
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Rendering left eye.");
            throw new ReportedException(crash);
        }
        try {
            entityRenderer.setEye(false);
            renderFrame(timer, image, getVideoWidth() / 2, 0);
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Rendering right eye.");
            throw new ReportedException(crash);
        }
        updateDefaultPreview(image);
        return image;
    }
}
