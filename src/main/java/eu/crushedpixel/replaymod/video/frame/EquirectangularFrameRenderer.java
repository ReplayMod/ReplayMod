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

    private static final byte IMAGE_BACK = 0;
    private static final byte IMAGE_FRONT = 1;
    private static final byte IMAGE_LEFT = 2;
    private static final byte IMAGE_RIGHT = 3;
    private static final byte IMAGE_TOP = 4;
    private static final byte IMAGE_BOTTOM = 5;

    private final int frameSize;
    protected final CubicEntityRenderer entityRenderer;

    private byte[][] image;
    private double[][] imageX;
    private double[][] imageY;

    public EquirectangularFrameRenderer(RenderOptions options, boolean stable) {
        super(options);
        this.frameSize = options.getWidth() / 4;
        this.entityRenderer = new CubicEntityRenderer(options, frameSize, stable);
        setCustomEntityRenderer(entityRenderer);
    }

    private void computeCubicToEquirectangularTables() {
        int rWidth = getVideoWidth();
        int rHeight = getVideoHeight();
        image = new byte[rWidth][rHeight];
        imageX = new double[rWidth][rHeight];
        imageY = new double[rWidth][rHeight];
        for (int i = 0; i < rWidth; i++) {
            double yaw = PI * 2 * i / rWidth;
            int piQuarter = 8 * i / rWidth - 4;
            byte target;
            if (piQuarter < -3) {
                target = IMAGE_BACK;
            } else if (piQuarter < -1) {
                target = IMAGE_LEFT;
            } else if (piQuarter < 1) {
                target = IMAGE_FRONT;
            } else if (piQuarter < 3) {
                target = IMAGE_RIGHT;
            } else {
                target = IMAGE_BACK;
            }
            double fYaw = (yaw + PI/4) % (PI / 2) - PI/4;
            double d = 1 / Math.cos(fYaw);
            double gcXN = (Math.tan(fYaw) + 1) / 2;
            for (int j = 0; j < rHeight; j++) {
                double cXN = gcXN;
                byte pt = target;
                double pitch = PI * j / rHeight - PI / 2;
                double cYN = (Math.tan(pitch) * d + 1) / 2;

                if (cYN >= 1) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (-Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = IMAGE_BOTTOM;
                }
                if (cYN < 0) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = IMAGE_TOP;
                }

                image[i][j] = pt;
                imageX[i][j] = Math.min(frameSize - 1, (cXN * frameSize));
                imageY[i][j] = Math.min(frameSize - 1, (cYN * frameSize));
            }
        }
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
        int[][] images = {
                ((DataBufferInt) back.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) front.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) left.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) right.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) top.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) bottom.getRaster().getDataBuffer()).getData()
        };

        int fWidth = frameSize;
        int rWidth = getVideoWidth();
        int rHeight = getVideoHeight();
        if (image == null || image.length != rWidth || image[0].length != rHeight) {
            try {
                computeCubicToEquirectangularTables();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        BufferedImage result = new BufferedImage(rWidth, rHeight, BufferedImage.TYPE_INT_RGB);
        int[] resultPixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        byte[] image;
        double[] imageX, imageY;
        for (int i = 0; i < rWidth; i++) {
            image = this.image[i];
            imageX = this.imageX[i];
            imageY = this.imageY[i];
            for (int j = 0; j < rHeight; j++) {
                resultPixels[i + j * rWidth] = images[image[j]][(int) imageX[j] + (int) imageY[j] * fWidth];
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
