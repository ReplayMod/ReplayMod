package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.video.frame.FrameRenderer;

import static org.apache.commons.lang3.Validate.*;

public final class RenderOptions {
    private FrameRenderer renderer;
    private float quality = 0.5f;
    private int fps = 30;
    private boolean waitForChunks = true;
    private boolean isLinearMovement = false;

    public RenderOptions(FrameRenderer renderer) {
        this.renderer = notNull(renderer);
    }

    public FrameRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(FrameRenderer renderer) {
        this.renderer = notNull(renderer);
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        inclusiveBetween(0f, 1f, quality);
        this.quality = quality;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        isTrue(fps > 0, "Fps must be positive.");
        this.fps = fps;
    }

    public boolean isWaitForChunks() {
        return waitForChunks;
    }

    public void setWaitForChunks(boolean waitForChunks) {
        this.waitForChunks = waitForChunks;
    }

    public boolean isLinearMovement() {
        return isLinearMovement;
    }

    public void setLinearMovement(boolean isLinearMovement) {
        this.isLinearMovement = isLinearMovement;
    }

    public RenderOptions copy() {
        RenderOptions copy = new RenderOptions(renderer);
        copy.quality = this.quality;
        copy.fps = this.fps;
        copy.waitForChunks = this.waitForChunks;
        copy.isLinearMovement = this.isLinearMovement;
        return copy;
    }
}
