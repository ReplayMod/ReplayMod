package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static java.lang.Math.PI;

public class EquirectangularFrameRenderer extends FrameRenderer {

    private final int frameSize;
    protected final CubicEntityRenderer entityRenderer;

    public EquirectangularFrameRenderer(RenderOptions options, boolean stable) {
        super(options);
        this.frameSize = options.getWidth() / 4;
        this.entityRenderer = new CubicEntityRenderer(options, frameSize, stable);
        setCustomEntityRenderer(entityRenderer);
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage top = captureFrame(timer, CubicEntityRenderer.Direction.TOP);
        BufferedImage bottom = captureFrame(timer, CubicEntityRenderer.Direction.BOTTOM);
        BufferedImage front = captureFrame(timer, CubicEntityRenderer.Direction.FRONT);
        BufferedImage back = captureFrame(timer, CubicEntityRenderer.Direction.BACK);
        BufferedImage left = captureFrame(timer, CubicEntityRenderer.Direction.LEFT);
        BufferedImage right = captureFrame(timer, CubicEntityRenderer.Direction.RIGHT);
        try {
            BufferedImage result = cubicToEquirectangular(top, bottom, front, back, left, right);
            updateDefaultPreview(result);
            return result;
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Transforming cubic to equirectangular image.");
            throw new ReportedException(crash);
        }
    }

    protected BufferedImage cubicToEquirectangular(BufferedImage top, BufferedImage bottom, BufferedImage front, BufferedImage back, BufferedImage left, BufferedImage right) {
        int rWidth = getVideoWidth();
        int rHeight = getVideoHeight();
        BufferedImage result = new BufferedImage(rWidth, rHeight, BufferedImage.TYPE_INT_RGB);
        int[] resultPixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < rWidth; i++) {
            double yaw = PI * 2 * i / rWidth;
            int piQuarter = 8 * i / rWidth - 4;
            BufferedImage target;
            if (piQuarter < -3) {
                target = back;
            } else if (piQuarter < -1) {
                target = left;
            } else if (piQuarter < 1) {
                target = front;
            } else if (piQuarter < 3) {
                target = right;
            } else {
                target = back;
            }
            double fYaw = (yaw + PI/4) % (PI / 2) - PI/4;
            double d = 1 / Math.cos(fYaw);
            double gcXN = (Math.tan(fYaw) + 1) / 2;
            for (int j = 0; j < rHeight; j++) {
                double cXN = gcXN;
                BufferedImage pt = target;
                double pitch = PI * j / rHeight - PI / 2;
                double cYN = (Math.tan(pitch) * d + 1) / 2;

                if (cYN >= 1) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (-Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = bottom;
                }
                if (cYN < 0) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = top;
                }

                int cX = Math.min(frameSize - 1, (int) (cXN * frameSize));
                int cY = Math.min(frameSize - 1, (int) (cYN * frameSize));
                resultPixels[i + j * rWidth] = 0xff000000 | pt.getRGB(cX, cY); // Make sure we got alpha value as well
            }
        }

        return result;
    }

    protected BufferedImage captureFrame(Timer timer, CubicEntityRenderer.Direction direction) {
        try {
            BufferedImage image = new BufferedImage(frameSize, frameSize, BufferedImage.TYPE_INT_RGB);
            entityRenderer.setDirection(direction);
            renderFrame(timer, image, 0, 0);
            return image;
        } catch (Throwable t) {
            CrashReport crash = CrashReport.makeCrashReport(t, "Rendering frame " + direction);
            throw new ReportedException(crash);
        }
    }
}
