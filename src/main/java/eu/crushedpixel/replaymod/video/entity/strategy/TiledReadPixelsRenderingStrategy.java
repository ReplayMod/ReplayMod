package eu.crushedpixel.replaymod.video.entity.strategy;

import eu.crushedpixel.replaymod.utils.OpenGLUtils;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class TiledReadPixelsRenderingStrategy implements FrameRenderingStrategy, CustomEntityRenderer.GluPerspectiveHook {

    private final CustomEntityRenderer renderer;
    private final int tileWidth;
    private final int tileHeight;
    private final ByteBuffer buffer;

    private int tileX;
    private int tileY;

    public TiledReadPixelsRenderingStrategy(CustomEntityRenderer renderer) {
        this.renderer = renderer;
        this.tileWidth = renderer.mc.displayWidth;
        this.tileHeight = renderer.mc.displayHeight;
        this.buffer = BufferUtils.createByteBuffer(tileWidth * tileHeight * 3);
        renderer.gluPerspectiveHook = this;
    }

    @Override
    public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        double height = Math.tan(fovY / 360 * Math.PI) * zNear;
        double width = height * aspect;

        double tilesX = (double) renderer.resultWidth / tileWidth;
        double tilesY = (double) renderer.resultHeight / tileHeight;
        double tilesMin = Math.min(tilesX, tilesY);
        scale(tilesMin, tilesMin, 1);
        double xOffset = (2 * tileX - tilesX + 1) / tilesMin;
        double yOffset = (2 * tileY - tilesY + 1) / tilesMin;
        translate(-xOffset, yOffset, 0);

        glFrustum(-width, width, -height, height, zNear, zFar);
    }

    @Override
    public void renderFrame(float partialTicks, BufferedImage into, int x, int y) {
        int tilesX = getTilesX();
        int tilesY = getTilesY();
        for (tileX = 0; tileX < tilesX; tileX++) {
            for (tileY = 0; tileY < tilesY; tileY++) {
                try {
                    renderTile(partialTicks, into, x + tileWidth * tileX, y + tileHeight * tileY);
                } catch (Throwable t) {
                    CrashReport crash = CrashReport.makeCrashReport(t, "Rendering tile " + x + "/" + y);
                    throw new ReportedException(crash);
                }
            }
        }
    }

    public void renderTile(final float partialTicks, BufferedImage into, int x, int y) {
        pushMatrix();

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();

        renderer.withDisplaySize(tileWidth, tileHeight, new Runnable() {
            @Override
            public void run() {
                renderer.renderWorld(partialTicks, 0);
            }
        });

        popMatrix();

        buffer.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, tileWidth, tileHeight, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        buffer.rewind();

        OpenGLUtils.openGlBytesToBufferedImage(buffer, tileWidth, into, x, y);
    }

    public int getTilesX() {
        int videoWidth = renderer.resultWidth;
        if (videoWidth % tileWidth == 0) {
            return videoWidth / tileWidth;
        } else {
            return videoWidth / tileWidth + 1;
        }
    }

    public int getTilesY() {
        int videoHeight = renderer.resultHeight;
        if (videoHeight % tileHeight == 0) {
            return videoHeight / tileHeight;
        } else {
            return videoHeight / tileHeight + 1;
        }
    }

    @Override
    public void cleanup() {

    }
}
