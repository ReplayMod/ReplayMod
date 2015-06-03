package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.minecraft.client.renderer.GlStateManager.bindTexture;
import static net.minecraft.client.renderer.GlStateManager.color;

public abstract class FrameRenderer {
    private static final ResourceLocation noPreviewTexture = new ResourceLocation("replaymod", "logo.jpg");
    protected final Minecraft mc = Minecraft.getMinecraft();
    protected final RenderOptions options;
    private CustomEntityRenderer customEntityRenderer;
    private DynamicTexture previewTexture;
    private boolean previewActive = false;
    protected final ByteBuffer buffer;

    public FrameRenderer(RenderOptions options) {
        this.options = options;
        this.buffer = BufferUtils.createByteBuffer(options.getWidth() * options.getHeight() * 3);
    }

    public final int getVideoWidth() {
        return options.getWidth();
    }

    public final int getVideoHeight() {
        return options.getHeight();
    }

    public void setPreviewActive(boolean previewActive) {
        this.previewActive = previewActive;
        if (previewActive) {
            if (previewTexture != null) {
                Arrays.fill(previewTexture.getTextureData(), 0xff000000);
                previewTexture.updateDynamicTexture();
            }
        }
    }

    public boolean isPreviewActive() {
        return previewActive;
    }

    public void setCustomEntityRenderer(CustomEntityRenderer customEntityRenderer) {
        this.customEntityRenderer = checkNotNull(customEntityRenderer);
    }

    public abstract BufferedImage captureFrame(Timer timer);

    protected void updateDefaultPreview(BufferedImage image) {
        if (isPreviewActive()) {
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            updateDefaultPreview(pixels);
        }
    }

    protected void updateDefaultPreview(int[] pixels) {
        if (isPreviewActive() && previewTexture != null) {
            System.arraycopy(pixels, 0, previewTexture.getTextureData(), 0, pixels.length);
            previewTexture.updateDynamicTexture();
        }
    }

    protected void renderFrame(Timer timer, BufferedImage into, int x, int y) {
        customEntityRenderer.renderFrame(timer.renderPartialTicks, into, x, y);
    }

    public void setup() {
        if (customEntityRenderer == null) {
            customEntityRenderer = new CustomEntityRenderer(options) {};
        }

        previewTexture = new DynamicTexture(getVideoWidth(), getVideoHeight()) {
            @Override
            public void updateDynamicTexture() {
                bindTexture(getGlTextureId());
                TextureUtil.uploadTextureSub(0, getTextureData(), getVideoWidth(), getVideoHeight(), 0, 0, true, false, false);
            }
        };
    }

    public void tearDown() {
        if (previewTexture != null) {
            previewTexture.deleteGlTexture();
        }
        if (customEntityRenderer != null) {
            customEntityRenderer.cleanup();
        }
    }

    public void drawPreviewTexture(int x, int y, int width, int height) {
        int videoWidth = getVideoWidth();
        int videoHeight = getVideoHeight();
        color(1, 1, 1, 1);
        if (previewTexture != null) {
            bindTexture(previewTexture.getGlTextureId());
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, videoWidth, videoHeight, width, height, videoWidth, videoHeight);
        }
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

        drawPreviewTexture(x, y, actualWidth, actualHeight);
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
