package com.replaymod.render.gui;

import com.replaymod.core.utils.Utils;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.VideoRenderer;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

//#if MC>=11400
import net.minecraft.client.texture.NativeImage;
//#endif

import java.nio.ByteBuffer;

import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

public class GuiVideoRenderer extends GuiScreen implements Tickable {
    private static final Identifier NO_PREVIEW_TEXTURE = identifier("replaymod", "logo.png");

    private final VideoRenderer renderer;

    public final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.rendering.title");
    public final GuiPanel imagePanel = new GuiPanel(){
        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (previewCheckbox.isChecked()) {
                renderPreview(renderer, size);
            } else {
                renderNoPreview(renderer, size);
            }
        }
    }.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
    public final GuiCheckbox previewCheckbox = new GuiCheckbox().setI18nLabel("replaymod.gui.rendering.preview");
    public final GuiLabel renderTime = new GuiLabel();
    public final GuiLabel remainingTime = new GuiLabel();
    public final GuiProgressBar progressBar = new GuiProgressBar();
    public final GuiPanel buttonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(4));
    public final GuiButton pauseButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            if (renderer.isPaused()) {
                pauseButton.setI18nLabel("replaymod.gui.rendering.pause");
                renderer.setPaused(false);
            } else {
                pauseButton.setI18nLabel("replaymod.gui.rendering.resume");
                renderer.setPaused(true);
            }
        }
    }).setI18nLabel("replaymod.gui.rendering.pause").setSize(150, 20);

    public final GuiButton cancelButton = new GuiButton(buttonPanel){
        boolean waitingForConfirmation;

        @Override
        public boolean mouseClick(ReadablePoint position, int button) {
            boolean result = super.mouseClick(position, button);
            if (waitingForConfirmation && !result) {
                setI18nLabel("replaymod.gui.rendering.cancel");
                waitingForConfirmation = false;
            }
            return result;
        }

        @Override
        public void onClick() {
            super.onClick();
            if (!waitingForConfirmation) {
                setI18nLabel("replaymod.gui.rendering.cancel.callback");
                waitingForConfirmation = true;
            } else {
                renderer.cancel();
            }
        }
    }.setI18nLabel("replaymod.gui.rendering.cancel").setSize(150, 20);

    private NativeImageBackedTexture previewTexture;
    private boolean previewTextureDirty;

    {
        final GuiPanel contentPanel = new GuiPanel(this).setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                size(progressBar, width, 20);
                pos(title, width / 2 - width(title) / 2, 0);
                pos(imagePanel, 0, y(title) + height(title) + 5);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height - height(buttonPanel));
                pos(progressBar, width / 2 - width(progressBar) / 2, y(buttonPanel) - 5 - height(progressBar));
                pos(renderTime, 0, y(progressBar) - 2 - height(renderTime));
                pos(remainingTime, width - width(remainingTime), y(progressBar) - 2 - height(renderTime));
                pos(previewCheckbox, width / 2 - width(previewCheckbox) / 2, y(renderTime) - 10 - height(previewCheckbox));
                size(imagePanel, width, y(previewCheckbox) - 5 - y(imagePanel));
            }
        }).addElements(null, title, imagePanel, previewCheckbox, renderTime, remainingTime, progressBar, buttonPanel);
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                // Make sure we have some space around our central GUI components
                pos(contentPanel, 5, 3);
                size(contentPanel, width - 10, height - 10);
            }
        });
        // FIXME default background doesn't work during rendering because the blur effect relies on the framebuffer
        //#if MC>=12006
        //$$ setBackground(Background.NONE);
        //$$ setBackgroundColor(new de.johni0702.minecraft.gui.utils.lwjgl.Color(32, 32, 32));
        //#else
        setBackground(Background.DIRT);
        //#endif
    }

    public GuiVideoRenderer(VideoRenderer renderer) {
        this.renderer = renderer;
    }

    //the total render time
    private int renderTimeTaken = 0;

    //the time at which the Screen was last updated
    private long prevTime = -1;

    //the time at which the rendering of the current frame started
    private long frameStartTime = -1;

    //the amount of frames that were rendered when the time left was last updated
    private int prevRenderedFrames = 0;

    //the estimated render time that is left (in seconds)
    private int renderTimeLeft = 0;

    //each of the durations the rendering process took for the last 50 frames
    private int[] renderTimes = new int[50];
    //the algorithm's current position in the renderTimes array
    private int currentIndex = 0;

    @Override
    public void tick() {
        long current = System.nanoTime() / 1_000_000;

        //first, update the total render time (only if rendering is not paused and has already started)
        if(!renderer.isPaused() && renderer.getFramesDone() > 0 && prevTime > -1) {
            renderTimeTaken += (current - prevTime);
        } else {
            //if the rendering process is paused, we have to update the frame start time to prevent huge render times
            //for the currently rendered frame(s)
            frameStartTime = current;
        }

        //always update prevTime, so we don't get huge time jumps when unpausing the rendering process
        prevTime = current;


        //calculate estimated time left

        //if the amount of rendered frames has increased since the last update
        if(prevRenderedFrames < renderer.getFramesDone()) {
            //we don't include the first frame in our calculations,
            //as setting up the rendering process takes a few seconds
            if(prevRenderedFrames > 0) {

                //calculate the amount of frames that have been rendered since the last update
                int framesRendered = renderer.getFramesDone() - prevRenderedFrames;

                //calculate the time it took to render these frames
                int renderTime = (int)(current - frameStartTime);

                //calculate the average time it took for each of these frames to render
                int avgRenderTime = renderTime/framesRendered;

                //add all of the average render times to the render times array
                for(int i=0; i<framesRendered; i++) {
                    renderTimes[currentIndex] = avgRenderTime;
                    currentIndex++;
                    if(currentIndex >= renderTimes.length) currentIndex = 0;
                }

                //the renderTimes array initially contains lots of zeros,
                //so we count the amount of valid timespans
                int validValues = 0;

                int totalTime = 0;
                for(int i : renderTimes) {
                    if(i > 0) {
                        totalTime += i;
                        validValues++;
                    }
                }

                //calculate the average render time for the previous [up to 50] frames
                float averageRenderTime = validValues > 0 ? totalTime / validValues : 0;

                //calculate the remaining render time in seconds
                renderTimeLeft = Math.round((averageRenderTime * (renderer.getTotalFrames() - renderer.getFramesDone())) / 1000);
            }

            //set the render start time of the next frame(s) to the current timestamp
            frameStartTime = current;
            //update the amount of rendered frames for the last calculation
            prevRenderedFrames = renderer.getFramesDone();
        }

        renderTime.setText(I18n.translate("replaymod.gui.rendering.timetaken") + ": " + secToString(renderTimeTaken/1000));
        remainingTime.setText(I18n.translate("replaymod.gui.rendering.timeleft") + ": " + secToString(renderTimeLeft));

        int framesDone = renderer.getFramesDone(), framesTotal = renderer.getTotalFrames();
        progressBar.setI18nLabel("replaymod.gui.rendering.progress", framesDone, framesTotal);
        progressBar.setProgress((float) framesDone / framesTotal);
    }

    private String secToString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(hours).append(I18n.translate("replaymod.gui.hours"));
        if(min > 0 || hours > 0) builder.append(min).append(I18n.translate("replaymod.gui.minutes"));
        builder.append(sec).append(I18n.translate("replaymod.gui.seconds"));

        return builder.toString();
    }

    private synchronized void renderPreview(GuiRenderer guiRenderer, ReadableDimension size) {
        ReadableDimension videoSize = renderer.getFrameSize();
        final int videoWidth = videoSize.getWidth();
        final int videoHeight = videoSize.getHeight();

        if (previewTexture == null) {
            //#if MC>=11400
            previewTexture = new NativeImageBackedTexture(videoWidth, videoHeight, true);
            //#else
            //$$ previewTexture = new DynamicTexture(videoWidth, videoHeight);
            //#endif
        }

        if (previewTextureDirty) {
            previewTexture.upload();
            previewTextureDirty = false;
        }

        guiRenderer.bindTexture(previewTexture.getGlId());
        renderPreviewTexture(guiRenderer, size, videoWidth, videoHeight);
    }

    private void renderNoPreview(GuiRenderer guiRenderer, ReadableDimension size) {
        guiRenderer.bindTexture(NO_PREVIEW_TEXTURE);
        renderPreviewTexture(guiRenderer, size, 1280, 720);
    }

    private void renderPreviewTexture(GuiRenderer guiRenderer, ReadableDimension size,
                                      int videoWidth, int videoHeight) {
        Dimension dimension = Utils.fitIntoBounds(new Dimension(videoWidth, videoHeight), size);

        int width = dimension.getWidth();
        int height = dimension.getHeight();
        int x = (size.getWidth() - width) / 2;
        int y = (size.getHeight() - height) / 2;

        guiRenderer.drawTexturedRect(x, y, 0, 0, width, height, videoWidth, videoHeight, videoWidth, videoHeight);
    }

    public void updatePreview(ByteBuffer buffer, ReadableDimension size) {
        if (previewCheckbox.isChecked() && previewTexture != null) {
            buffer.mark();
            synchronized (this) {
                //#if MC>=11400
                NativeImage data = previewTexture.getImage();
                assert data != null;
                //#else
                //$$ int[] data = previewTexture.getTextureData();
                //#endif
                // Note: Optifine changes the texture data array to be three times as long (for use by shaders),
                //       we only want to initialize the first third and since we use our frame size, not the array size,
                //       we're good to go.
                int width = size.getWidth();
                for (int y = 0; y < size.getHeight(); y++) {
                    for (int x = 0; x < width; x++) {
                        int b = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int r = buffer.get() & 0xff;
                        buffer.get(); // alpha
                        //#if MC>=12102
                        //$$ int value = 0xff << 24 | r << 16 | g << 8 |  b;
                        //$$ data.setColorArgb(x, y, value);
                        //#elseif MC>=11400
                        int value = 0xff << 24 | b << 16 | g << 8 |  r;
                        data.setPixelColor(x, y, value); // actually takes ABGR, not RGBA
                        //#else
                        //$$ int value = 0xff << 24 | r << 16 | g << 8 |  b;
                        //$$ data[y * width + x] = value;
                        //#endif
                    }
                }
                previewTextureDirty = true;
            }
            buffer.reset();
        }
    }
}
