package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.gui.elements.GuiProgressBar;
import eu.crushedpixel.replaymod.utils.BoundingUtils;
import eu.crushedpixel.replaymod.utils.DurationUtils;
import eu.crushedpixel.replaymod.video.VideoRenderer;
import eu.crushedpixel.replaymod.video.frame.RGBFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.bindTexture;
import static net.minecraft.client.renderer.GlStateManager.color;

public class GuiVideoRenderer extends GuiScreen {
    private static final ResourceLocation noPreviewTexture = new ResourceLocation("replaymod", "logo.jpg");

    private final VideoRenderer renderer;

    private final String SCREEN_TITLE = I18n.format("replaymod.gui.rendering.title");
    private final String PAUSE_RENDERING = I18n.format("replaymod.gui.rendering.pause");
    private final String RESUME_RENDERING = I18n.format("replaymod.gui.rendering.resume");
    private final String CANCEL = I18n.format("replaymod.gui.rendering.cancel");
    private final String CANCEL_CONFIRM = I18n.format("replaymod.gui.rendering.cancel.callback");
    private final String PREVIEW = I18n.format("replaymod.gui.rendering.preview");

    private GuiButton pauseButton;
    private GuiButton cancelButton;
    private GuiCheckBox previewCheckBox;
    private GuiProgressBar progressBar;

    private DynamicTexture previewTexture;
    private boolean previewTextureDirty;

    private int renderTimeTaken = 0;
    private long prevTime = -1;
    private long prevRenderTime = -1;

    private int[] renderTimes = new int[50];
    private int currentIndex = 0;

    private int prevRenderedFrames = 0;
    private int renderTimeLeft = 0;

    public GuiVideoRenderer(VideoRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    @SuppressWarnings("unchecked") // I blame forge for not re-adding generics to that list
    public void initGui() {
        String text = PAUSE_RENDERING;
        buttonList.add(pauseButton = new GuiButton(1, width / 2 - 152, height - 10 - 20, text));

        text = CANCEL;
        buttonList.add(cancelButton = new GuiButton(1, width / 2 + 2, height - 10 - 20, text));

        pauseButton.width = cancelButton.width = 150;

        progressBar = new GuiProgressBar(10, height - 10 - 20 - 10 - 20, width-20, 20);

        text = PREVIEW;
        buttonList.add(previewCheckBox = new GuiCheckBox(0, (width - fontRendererObj.getStringWidth(text)) / 2 - 8,
                pauseButton.yPosition - 10 - 20 - 10 - 20 - 5 , text, false));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        String cancelBefore = cancelButton.displayString;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        String cancelAfter = cancelButton.displayString;
        if (cancelBefore.equals(cancelAfter)) {
            cancelButton.displayString = CANCEL;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == pauseButton) {
            if (PAUSE_RENDERING.equals(pauseButton.displayString)) {
                renderer.setPaused(true);
                pauseButton.displayString = RESUME_RENDERING;
            } else if (RESUME_RENDERING.equals(pauseButton.displayString)) {
                renderer.setPaused(false);
                pauseButton.displayString = PAUSE_RENDERING;
            } else {
                throw new IllegalStateException(pauseButton.displayString);
            }
        } else if (button == cancelButton) {
            if (CANCEL.equals(cancelButton.displayString)) {
                cancelButton.displayString = CANCEL_CONFIRM;
            } else if (CANCEL_CONFIRM.equals(cancelButton.displayString)) {
                renderer.cancel();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long current = System.currentTimeMillis();

        //calculate estimated time left
        if(prevRenderTime == -1) {
            prevRenderTime = current;
            prevTime = current;
        }

        if(prevRenderedFrames < renderer.getFramesDone()) {
            prevRenderedFrames = renderer.getFramesDone();

            int renderTime = (int)(current - prevRenderTime);

            renderTimes[currentIndex] = renderTime;

            int validValues = 0;

            int totalTime = 0;
            for(int i : renderTimes) {
                if(i > 0) {
                    totalTime += i;
                    validValues++;
                }
            }

            float averageRenderTime = validValues > 0 ? totalTime / validValues : 0;

            prevRenderTime = current;

            //remaining render time in seconds
            renderTimeLeft = Math.round((averageRenderTime * (renderer.getTotalFrames() - renderer.getFramesDone())) / 1000);
        }

        if(!renderer.isPaused()) {
            renderTimeTaken += (current - prevTime);
        }

        prevTime = current;

        currentIndex++;
        if(currentIndex >= renderTimes.length) currentIndex = 0;

        String takenString = I18n.format("replaymod.gui.rendering.timetaken")+": "+DurationUtils.convertSecondsToString(renderTimeTaken/1000);
        String leftString = I18n.format("replaymod.gui.rendering.timeleft")+": "+DurationUtils.convertSecondsToString(renderTimeLeft);

        int centerX = width / 2;

        drawBackground(0);

        drawCenteredString(fontRendererObj, SCREEN_TITLE, centerX, 5, Color.WHITE.getRGB());

        String framesProgress = I18n.format("replaymod.gui.rendering.progress", renderer.getFramesDone(), renderer.getTotalFrames());
        progressBar.setProgressString(framesProgress);
        progressBar.setProgress((float) renderer.getFramesDone() / renderer.getTotalFrames());

        progressBar.drawProgressBar();

        int previewX = 10;
        int previewWidth = width - 20;

        int previewHeight = previewCheckBox.yPosition - 10 - 20;
        int previewY = previewCheckBox.yPosition - 10 - previewHeight;

        if(previewCheckBox.isChecked()) {
            renderPreview(previewX, previewY, previewWidth, previewHeight);
        } else {
            renderNoPreview(previewX, previewY, previewWidth, previewHeight);
        }

        drawString(fontRendererObj, takenString, 12, previewCheckBox.yPosition + 5 + 20, Color.WHITE.getRGB());
        drawString(fontRendererObj, leftString, width - 12 - fontRendererObj.getStringWidth(leftString),
                previewCheckBox.yPosition + 5 + 20, Color.WHITE.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private synchronized void renderPreview(int x, int y, int width, int height) {
        ReadableDimension videoSize = renderer.getFrameSize();
        final int videoWidth = videoSize.getWidth();
        final int videoHeight = videoSize.getHeight();

        if (previewTexture == null) {
            previewTexture = new DynamicTexture(videoWidth, videoHeight) {
                @Override
                public void updateDynamicTexture() {
                    bindTexture(getGlTextureId());
                    TextureUtil.uploadTextureSub(0, getTextureData(), videoWidth, videoHeight, 0, 0, true, false, false);
                }
            };
        }

        if (previewTextureDirty) {
            previewTexture.updateDynamicTexture();
            previewTextureDirty = false;
        }

        Dimension dimension = BoundingUtils.fitIntoBounds(new Dimension(videoSize), new Dimension(width, height));

        x += (width - dimension.getWidth()) / 2;
        y += (height - dimension.getHeight()) / 2;
        width = dimension.getWidth();
        height = dimension.getHeight();

        color(1, 1, 1, 1);
        bindTexture(previewTexture.getGlTextureId());
        drawScaledCustomSizeModalRect(x, y, 0, 0, videoWidth, videoHeight, width, height, videoWidth, videoHeight);
    }

    private void renderNoPreview(int x, int y, int width, int height) {
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
        drawScaledCustomSizeModalRect(x, y, 0, 0, 1280, 720, actualWidth, actualHeight, 1280, 720);
    }

    public void updatePreview(RGBFrame frame) {
        if (previewCheckBox.isChecked() && previewTexture != null) {
            ByteBuffer buffer = frame.getByteBuffer();
            buffer.mark();
            synchronized (this) {
                int[] data = previewTexture.getTextureData();
                for (int i = 0; i < data.length; i++) {
                    data[i] = 0xff << 24 | (buffer.get() & 0xff) << 16 | (buffer.get() & 0xff) << 8 |  (buffer.get() & 0xff);
                }
                previewTextureDirty = true;
            }
            buffer.reset();
        }
    }
}
