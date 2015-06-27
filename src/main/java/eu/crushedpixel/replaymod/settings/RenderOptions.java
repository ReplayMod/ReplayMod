package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.video.frame.FrameRenderer;
import net.minecraft.client.Minecraft;

import static org.apache.commons.lang3.Validate.*;

public final class RenderOptions {
    private FrameRenderer renderer;
    private String bitrate = "10M";
    private int fps = 30;

    // Advanced
    private boolean waitForChunks = true;
    private boolean isLinearMovement = false;
    private int skyColor = -1;
    private int width = Minecraft.getMinecraft().displayWidth;
    private int height = Minecraft.getMinecraft().displayHeight;

    // Highly advanced
    private String exportCommand = "ffmpeg";
    private String exportCommandArgs = "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - " +
            "-an " +
            "-c:v libvpx -b:v %BITRATE% %FILENAME%.webm";
    private int writerQueueSize = Integer.parseInt(System.getProperty("replaymod.render.writerQueueSize", "1"));

    public FrameRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(FrameRenderer renderer) {
        this.renderer = notNull(renderer);
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
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

    public boolean isDefaultSky() {
        return skyColor == -1;
    }

    public int getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(int skyColor) {
        this.skyColor = skyColor;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getExportCommand() {
        return exportCommand;
    }

    public void setExportCommand(String exportCommand) {
        this.exportCommand = exportCommand;
    }

    public String getExportCommandArgs() {
        return exportCommandArgs;
    }

    public void setExportCommandArgs(String exportCommandArgs) {
        this.exportCommandArgs = exportCommandArgs;
    }

    public int getWriterQueueSize() {
        return writerQueueSize;
    }

    public void setWriterQueueSize(int writerQueueSize) {
        this.writerQueueSize = writerQueueSize;
    }

    public RenderOptions copy() {
        RenderOptions copy = new RenderOptions();
        copy.renderer = this.renderer;
        copy.bitrate = this.bitrate;
        copy.fps = this.fps;
        copy.waitForChunks = this.waitForChunks;
        copy.isLinearMovement = this.isLinearMovement;
        copy.skyColor = this.skyColor;
        copy.width = this.width;
        copy.height = this.height;
        copy.exportCommand = this.exportCommand;
        copy.exportCommandArgs = this.exportCommandArgs;
        copy.writerQueueSize = this.writerQueueSize;
        return copy;
    }
}
