package eu.crushedpixel.replaymod.video.frame;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Runnables;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public abstract class FrameRenderer {
    private static final ResourceLocation noPreviewTexture = new ResourceLocation("replaymod", "logo.jpg");
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final int videoWidth;
    private final int videoHeight;
    private CustomEntityRenderer customEntityRenderer;
    private DynamicTexture previewTexture;
    private Runnable renderPreviewCallback;
    private boolean previewActive = false;
    protected final ByteBuffer buffer;

    public FrameRenderer(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.buffer = BufferUtils.createByteBuffer(mc.displayWidth * mc.displayHeight * 3);
        this.customEntityRenderer = new CustomEntityRenderer() {};
    }

    public FrameRenderer(int videoWidth, int videoHeight, int bufferSize) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.buffer = BufferUtils.createByteBuffer(bufferSize);
        this.customEntityRenderer = new CustomEntityRenderer() {};
    }

    public final int getVideoWidth() {
        return videoWidth;
    }

    public final int getVideoHeight() {
        return videoHeight;
    }

    public void setPreviewActive(boolean previewActive) {
        this.previewActive = previewActive;
        if (previewActive) {
            Arrays.fill(previewTexture.getTextureData(), 0xff000000);
            previewTexture.updateDynamicTexture();
        }
    }

    public boolean isPreviewActive() {
        return previewActive;
    }

    public boolean setRenderPreviewCallback(Runnable renderPreviewCallback) {
        Preconditions.checkState(this.renderPreviewCallback == null, "Render preview callback already set.");
        this.renderPreviewCallback = renderPreviewCallback;
        return false;
    }

    public Runnable getRenderPreviewCallback() {
        return renderPreviewCallback != null ? renderPreviewCallback : Runnables.doNothing();
    }

    public void setCustomEntityRenderer(CustomEntityRenderer customEntityRenderer) {
        this.customEntityRenderer = checkNotNull(customEntityRenderer);
    }

    public abstract BufferedImage captureFrame(Timer timer);

    protected void readPixels(ByteBuffer buffer) {
        buffer.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        buffer.rewind();
    }

    protected void updateDefaultPreview(BufferedImage image) {
        if (isPreviewActive()) {
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            updateDefaultPreview(pixels);
        }
    }

    protected void updateDefaultPreview(int[] pixels) {
        if (isPreviewActive()) {
            System.arraycopy(pixels, 0, getPreviewTexture().getTextureData(), 0, pixels.length);
            getPreviewTexture().updateDynamicTexture();
        }
    }

    protected void copyPixelsToImage(ByteBuffer buffer, int bufferWidth, BufferedImage image, int offsetX, int offsetY) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        copyPixelsToImage(buffer, bufferWidth, pixels, offsetX, offsetY);
    }

    protected void copyPixelsToImage(ByteBuffer buffer, int bufferWidth, BufferedImage image, int imageWidth, int offsetX, int offsetY) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        copyPixelsToImage(buffer, bufferWidth, pixels, imageWidth, offsetX, offsetY);
    }

    protected void copyPixelsToImage(ByteBuffer buffer, int bufferWidth, int[] image, int offsetX, int offsetY) {
        copyPixelsToImage(buffer, bufferWidth, image, videoWidth, offsetX, offsetY);
    }

    protected void copyPixelsToImage(ByteBuffer buffer, int bufferWidth, int[] image, int imageWidth, int offsetX, int offsetY) {
        int bufferSize = buffer.remaining() / 3;
        // Read the OpenGL image row by row from right to left (flipped horizontally)
        for (int i = bufferSize - 1; i >= 0; i--) {
            // Coordinates in the final image
            int x = offsetX + bufferWidth - i % bufferWidth - 1; // X coord of OpenGL image has to be flipped first
            int y = offsetY + i / bufferWidth;
            if (x >= imageWidth || y * imageWidth >= image.length) {
                buffer.position(buffer.position() + 3); // Pixel would end up outside of image
                continue;
            }
            // Write to image (row by row, left to right)
            image[y * imageWidth + x] = 0xff << 24 | (buffer.get() & 0xff) << 16 | (buffer.get() & 0xff) << 8 | buffer.get() & 0xff;
        }
        buffer.rewind();
    }

    protected void renderFrame(Timer timer) {
        pushMatrix();
        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        mc.getFramebuffer().bindFramebuffer(true);

        enableTexture2D();

        customEntityRenderer.updateCameraAndRender(timer.renderPartialTicks);

        mc.getFramebuffer().unbindFramebuffer();
        popMatrix();

        pushMatrix();
        mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
        popMatrix();
    }

    public void setup() {
        this.previewTexture = new DynamicTexture(videoWidth, videoHeight) {
            @Override
            public void updateDynamicTexture() {
                bindTexture(getGlTextureId());
                TextureUtil.uploadTextureSub(0, getTextureData(), getVideoWidth(), getVideoHeight(), 0, 0, true, false, false);
            }
        };
    }

    public void tearDown() {
        this.previewTexture.deleteGlTexture();
    }

    public DynamicTexture getPreviewTexture() {
        return previewTexture;
    }

    /**
     * Draw a preview of the current scene within the specified box.
     * @param x Left border of the box
     * @param y Upper border of the box
     * @param width Width of the box
     * @param height Height of the box
     */
    public void renderPreview(int x, int y, int width, int height) {
        int actualWidth = width;
        int actualHeight = height;
        if (width / height > getVideoWidth() / getVideoHeight()) {
            actualWidth = height * getVideoWidth() / getVideoHeight();
        } else {
            actualHeight = width * getVideoHeight() / getVideoWidth();
        }

        x += (width - actualWidth) / 2;
        y += (height - actualHeight) / 2;

        bindTexture(previewTexture.getGlTextureId());
        color(1, 1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, videoWidth, videoHeight, actualWidth, actualHeight, videoWidth, videoHeight);
    }

    public static void renderNoPreview(int x, int y, int width, int height) {
        int actualWidth = width;
        int actualHeight = height;
        if (width / height > 1280 / 720) {
            actualWidth = height * 1280 / 720;
        } else {
            actualHeight = width * 720 / 1280;
        }

        x += (width - actualWidth) / 2;
        y += (height - actualHeight) / 2;

        Minecraft.getMinecraft().getTextureManager().bindTexture(noPreviewTexture);
        color(1, 1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 1280, 720, actualWidth, actualHeight, 1280, 720);
    }
}
