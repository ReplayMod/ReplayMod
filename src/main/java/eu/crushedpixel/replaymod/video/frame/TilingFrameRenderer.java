package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.entity.TilingEntityRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class TilingFrameRenderer extends FrameRenderer {
    private final TilingEntityRenderer entityRenderer = new TilingEntityRenderer(this);
    private final int tileWidth;
    private final int tileHeight;

    public TilingFrameRenderer(int videoWidth, int videoHeight) {
        super(videoWidth, videoHeight);
        this.tileWidth = mc.displayWidth;
        this.tileHeight = mc.displayHeight;
        setCustomEntityRenderer(entityRenderer);
    }

    @Override
    public boolean setRenderPreviewCallback(Runnable renderPreviewCallback) {
        super.setRenderPreviewCallback(renderPreviewCallback);
        return true;
    }

    @Override
    public BufferedImage captureFrame(Timer timer) {
        BufferedImage image = new BufferedImage(getVideoWidth(), getVideoHeight(), BufferedImage.TYPE_INT_RGB);
        int tilesX = getTilesX();
        int tilesY = getTilesY();
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                try {
                    captureTile(timer, x, y, image);
                } catch (Throwable t) {
                    CrashReport crash = CrashReport.makeCrashReport(t, "Rendering tile " + x + "/" + y);
                    throw new ReportedException(crash);
                }
                getRenderPreviewCallback().run();
            }
        }
        return image;
    }

    protected void captureTile(Timer timer, int tileX, int tileY, BufferedImage into) {
        // Render frame
        entityRenderer.setTile(tileX, tileY);
        renderFrame(timer);

        // Read pixels
        readPixels(buffer);

        // Copy to image
        copyPixelsToImage(buffer, tileWidth, into, tileX * tileWidth, tileY * tileHeight);
        if (isPreviewActive()) {
            int[] pixels = getPreviewTexture().getTextureData();
            copyPixelsToImage(buffer, tileWidth, pixels, tileX * tileWidth, tileY * tileHeight);

            // Draw red rectangle around next tile
            tileY = (tileY + 1) % getTilesY();
            if (tileY == 0) {
                tileX = (tileX + 1) % getTilesX();
            }
            drawPreviewRectangle(pixels, tileX, tileY);

            getPreviewTexture().updateDynamicTexture();
        }
    }

    private void drawPreviewRectangle(int[] pixels, int tileX, int tileY) {
        final int RED = 0xffff0000;

        int videoWidth = getVideoWidth();
        int videoHeight = getVideoHeight();
        int border = videoWidth / tileWidth * 5;
        int left = Math.min(tileX * tileWidth, videoWidth - border - 1);
        int right = Math.min((left + tileWidth), videoWidth) - 1;
        int top = Math.min(tileY * tileHeight, videoHeight - border - 1);
        int bottom = Math.min((top + tileHeight), videoHeight - border) - 1;

        for (int i = 0; i < border; i++) {
            // Top border
            int offset = (top + i) * videoWidth;
            Arrays.fill(pixels, offset + left, offset + right + 1, RED);

            // Bottom border
            offset = (bottom - i) * videoWidth;
            Arrays.fill(pixels, offset + left, offset + right + 1, RED);

            // Left border
            for (int j = top; j <= bottom; j++) {
                pixels[j * videoWidth + left + i] = RED;
            }

            // Right border
            for (int j = top; j <= bottom; j++) {
                pixels[j * videoWidth + right - i] = RED;
            }
        }
    }

    public int getTilesX() {
        int videoWidth = getVideoWidth();
        if (videoWidth % tileWidth == 0) {
            return videoWidth / tileWidth;
        } else {
            return videoWidth / tileWidth + 1;
        }
    }

    public int getTilesY() {
        int videoHeight = getVideoHeight();
        if (videoHeight % tileHeight == 0) {
            return videoHeight / tileHeight;
        } else {
            return videoHeight / tileHeight + 1;
        }
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }
}
