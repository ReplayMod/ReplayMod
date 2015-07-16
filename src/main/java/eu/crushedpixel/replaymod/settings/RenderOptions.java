package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import lombok.Data;
import net.minecraft.client.Minecraft;

import java.io.File;

import static org.apache.commons.lang3.Validate.isTrue;

@Data
public final class RenderOptions {
    private Pipelines.Preset mode = Pipelines.Preset.DEFAULT;
    private String bitrate = "10M";
    private int fps = 30;

    private File outputFile;

    private boolean ignoreCameraRotation;

    // Advanced
    private boolean waitForChunks = true;
    private boolean isLinearMovement = false;
    private boolean hideNameTags = true;
    private int skyColor = -1;
    private int width = Minecraft.getMinecraft().displayWidth;
    private int height = Minecraft.getMinecraft().displayHeight;

    // Highly advanced
    private boolean highPerformance;
    private String exportCommand = "ffmpeg";
    private String exportCommandArgs = "";

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
        copy.mode = this.mode;
        copy.bitrate = this.bitrate;
        copy.fps = this.fps;
        copy.ignoreCameraRotation = this.ignoreCameraRotation;
        copy.waitForChunks = this.waitForChunks;
        copy.isLinearMovement = this.isLinearMovement;
        copy.hideNameTags = this.hideNameTags;
        copy.skyColor = this.skyColor;
        copy.width = this.width;
        copy.height = this.height;
        copy.highPerformance = this.highPerformance;
        copy.exportCommand = this.exportCommand;
        copy.exportCommandArgs = this.exportCommandArgs;
        return copy;
    }
}
