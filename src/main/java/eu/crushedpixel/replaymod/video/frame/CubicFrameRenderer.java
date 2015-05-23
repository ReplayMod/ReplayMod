package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;

public class CubicFrameRenderer extends FrameRenderer {
    protected static int getDisplaySize() {
        Minecraft mc = Minecraft.getMinecraft();
        return Math.min(mc.displayWidth, mc.displayHeight);
    }

    protected final CubicEntityRenderer entityRenderer;
    protected final int displaySize;

    protected CubicFrameRenderer(int videoWidth, int videoHeight, boolean stable) {
        super(videoWidth, videoHeight, getDisplaySize() * getDisplaySize() * 3);
        this.displaySize = getDisplaySize();
        this.entityRenderer = new CubicEntityRenderer(stable);
        setCustomEntityRenderer(entityRenderer);
    }

    public CubicFrameRenderer(boolean stable) {
        this(getDisplaySize() * 4, getDisplaySize() * 3, stable);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage image = new BufferedImage(getVideoWidth(), getVideoHeight(), BufferedImage.TYPE_INT_RGB);
        for (CubicEntityRenderer.Direction direction : CubicEntityRenderer.Direction.values()) {
            try {
                captureFrame(timer, image, direction);
            } catch (Throwable t) {
                CrashReport crash = CrashReport.makeCrashReport(t, "Rendering frame " + direction + ".");
                throw new ReportedException(crash);
            }
        }
        updateDefaultPreview(image);
        return image;
    }

    protected void captureFrame(Timer timer, BufferedImage into, CubicEntityRenderer.Direction direction) {
        renderFrameToBuffer(timer, direction);

        // Copy to image
        int frame = direction.getCubicFrame();
        int xVideo = frame % 4 * displaySize;
        int yVideo = frame / 4 * displaySize;
        copyPixelsToImage(buffer, displaySize, into, xVideo, yVideo);
    }

    protected void renderFrameToBuffer(Timer timer, CubicEntityRenderer.Direction direction) {
        // Setup aspect ratio
        int originalWidth = mc.displayWidth;
        int originalHeight = mc.displayHeight;
        mc.displayWidth = mc.displayHeight = displaySize;
        updateFrameBufferSize();

        // Render frame
        entityRenderer.setDirection(direction);
        renderFrame(timer);

        // Read pixels
        readPixels(buffer);

        mc.displayWidth = originalWidth;
        mc.displayHeight = originalHeight;
        updateFrameBufferSize();
    }

    private void updateFrameBufferSize() {
        mc.getFramebuffer().createBindFramebuffer(mc.displayWidth, mc.displayHeight);
        mc.entityRenderer.updateShaderGroupSize(mc.displayWidth, mc.displayHeight);
    }
}
