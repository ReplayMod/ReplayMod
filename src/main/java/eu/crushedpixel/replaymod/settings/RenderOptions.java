package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.video.frame.FrameRenderer;
import lombok.Data;
import net.minecraft.client.Minecraft;

import static org.apache.commons.lang3.Validate.*;

@Data
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

    public void setRenderer(FrameRenderer renderer) {
        this.renderer = notNull(renderer);
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        isTrue(fps > 0, "Fps must be positive.");
        this.fps = fps;
    }

    public boolean isDefaultSky() {
        return skyColor == -1;
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
