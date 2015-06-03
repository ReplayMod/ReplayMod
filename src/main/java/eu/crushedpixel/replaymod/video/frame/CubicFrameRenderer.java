package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class CubicFrameRenderer extends FrameRenderer {

    protected final int frameSize;
    protected final CubicEntityRenderer entityRenderer;

    protected CubicFrameRenderer(RenderOptions options, int frameSize, boolean stable) {
        super(options);
        this.frameSize = frameSize;
        this.entityRenderer = new CubicEntityRenderer(options, frameSize, stable);
        setCustomEntityRenderer(entityRenderer);
    }

    public CubicFrameRenderer(RenderOptions options, boolean stable) {
        this(options, options.getWidth() / 4, stable);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage image = new BufferedImage(getVideoWidth(), getVideoHeight(), BufferedImage.TYPE_INT_RGB);
        for (CubicEntityRenderer.Direction direction : CubicEntityRenderer.Direction.values()) {
            try {
                entityRenderer.setDirection(direction);

                int frame = direction.getCubicFrame();
                int xVideo = frame % 4 * frameSize;
                int yVideo = frame / 4 * frameSize;
                renderFrame(timer, image, xVideo, yVideo);
            } catch (Throwable t) {
                CrashReport crash = CrashReport.makeCrashReport(t, "Rendering frame " + direction + ".");
                throw new ReportedException(crash);
            }
        }
        updateDefaultPreview(image);
        return image;
    }
}
