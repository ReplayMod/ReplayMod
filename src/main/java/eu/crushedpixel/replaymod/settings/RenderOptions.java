package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Arrays;

import static org.apache.commons.lang3.Validate.isTrue;

@Data
public final class RenderOptions {
    private Pipelines.Preset mode = Pipelines.Preset.DEFAULT;
    private String bitrate = "10000K";
    private int fps = 30;

    private File outputFile;

    /**
     * Whether to ignore camera rotation. On yaw, pitch, roll axis.
     */
    private boolean[] ignoreCameraRotation = new boolean[3];

    // Advanced
    private boolean waitForChunks = true;
    private boolean isLinearMovement = false;
    private boolean hideNameTags = false;
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

    public void setIgnoreCameraRotation(boolean yaw, boolean pitch, boolean roll) {
        ignoreCameraRotation = new boolean[]{yaw, pitch, roll};
    }

    public RenderOptions copy() {
        RenderOptions copy = new RenderOptions();
        copy.mode = this.mode;
        copy.bitrate = this.bitrate;
        copy.fps = this.fps;
        copy.ignoreCameraRotation = Arrays.copyOf(this.ignoreCameraRotation, 3);
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

    /*/
    /* Methods and Fields used to save/load a RenderOptions instance from/to a Configuration File
    /*/

    private static final String CATEGORY_NAME = "settings_cache";

    private static final String MODE_ENTRY = "mode";
    private static final String BITRATE_ENTRY = "bitrate";
    private static final String FPS_ENTRY = "fps";
    private static final String IGNORE_ROTATION_ENTRY = "ignoreRotation";
    private static final String CHUNKS_ENTRY = "waitForChunks";
    private static final String LINEAR_ENTRY = "linearMovement";
    private static final String HIDE_NAMETAGS_ENTRY = "hideNametags";
    private static final String SKY_COLOR_ENTRY = "skyColor";
    private static final String WIDTH_ENTRY = "xRes";
    private static final String HEIGHT_ENTRY = "yRes";
    private static final String HIGH_PERFORMANCE_ENTRY = "highPerformance";
    private static final String EXPORT_COMMAND_ENTRY = "exportCommand";
    private static final String EXPORT_COMMAND_ARGS_ENTRY = "exportCommandArgs";

    public static RenderOptions loadFromConfig(Configuration config) {
        RenderOptions loaded = new RenderOptions();

        config.load();

        loaded.mode = getModeFromConfig(config);
        loaded.bitrate = getBitrateFromConfig(config);
        loaded.fps = getFramerateFromConfig(config);
        loaded.ignoreCameraRotation = getIgnoreCameraRotationFromConfig(config);
        loaded.waitForChunks = getWaitForChunksFromConfig(config);
        loaded.isLinearMovement = getLinearFromConfig(config);
        loaded.hideNameTags = getHideNametagsFromConfig(config);
        loaded.skyColor = getSkyColorFromConfig(config);
        loaded.width = getWidthFromConfig(config);
        loaded.height = getHeightFromConfig(config);
        loaded.highPerformance = getHighPerformanceFromConfig(config);
        loaded.exportCommand = getExportCommandFromConfig(config);
        loaded.exportCommandArgs = getExportCommandArgsFromConfig(config);

        return loaded;
    }

    public void saveToConfig(Configuration config) {
        config.load();

        config.removeCategory(config.getCategory(CATEGORY_NAME));

        getModeFromConfig(config, getMode().name());
        getBitrateFromConfig(config, getBitrate());
        getFramerateFromConfig(config, getFps());
        getIgnoreCameraRotationFromConfig(config, getIgnoreCameraRotation());
        getWaitForChunksFromConfig(config, isWaitForChunks());
        getLinearFromConfig(config, isLinearMovement());
        getHideNametagsFromConfig(config, isHideNameTags());
        getSkyColorFromConfig(config, getSkyColor());
        getWidthFromConfig(config, getWidth());
        getHeightFromConfig(config, getHeight());
        getHighPerformanceFromConfig(config, isHighPerformance());
        getExportCommandFromConfig(config, getExportCommand());
        getExportCommandArgsFromConfig(config, getExportCommandArgs());

        config.save();
    }

    private static Pipelines.Preset getModeFromConfig(Configuration config) {
        return getModeFromConfig(config, Pipelines.Preset.DEFAULT.name());
    }

    private static Pipelines.Preset getModeFromConfig(Configuration config, String def) {
        return Pipelines.Preset.valueOf(config.get(CATEGORY_NAME, MODE_ENTRY, def).getString());
    }

    private static String getBitrateFromConfig(Configuration config) {
        return getBitrateFromConfig(config, "10000K");
    }

    private static String getBitrateFromConfig(Configuration config, String def) {
        return config.get(CATEGORY_NAME, BITRATE_ENTRY, def).getString();
    }

    private static int getFramerateFromConfig(Configuration config) {
        return getFramerateFromConfig(config, 30);
    }

    private static int getFramerateFromConfig(Configuration config, int def) {
        return config.get(CATEGORY_NAME, FPS_ENTRY, def).getInt();
    }

    private static boolean[] getIgnoreCameraRotationFromConfig(Configuration config) {
        return getIgnoreCameraRotationFromConfig(config, new boolean[3]);
    }

    private static boolean[] getIgnoreCameraRotationFromConfig(Configuration config, boolean[] def) {
        String converted = "";
        for(boolean b : def) converted += b+" ";

        String result = config.get(CATEGORY_NAME, IGNORE_ROTATION_ENTRY, converted).getString();

        String[] split = result.split(" ");

        boolean[] ret = new boolean[3];
        for(int i=0; i<3; i++) {
            ret[i] = Boolean.valueOf(split[i]);
        }

        return ret;
    }

    private static boolean getWaitForChunksFromConfig(Configuration config) {
        return getWaitForChunksFromConfig(config, true);
    }

    private static boolean getWaitForChunksFromConfig(Configuration config, boolean def) {
        return config.get(CATEGORY_NAME, CHUNKS_ENTRY, def).getBoolean();
    }

    private static boolean getLinearFromConfig(Configuration config) {
        return getLinearFromConfig(config, false);
    }

    private static boolean getLinearFromConfig(Configuration config, boolean def) {
        return config.get(CATEGORY_NAME, LINEAR_ENTRY, def).getBoolean();
    }

    private static boolean getHideNametagsFromConfig(Configuration config) {
        return getHideNametagsFromConfig(config, false);
    }

    private static boolean getHideNametagsFromConfig(Configuration config, boolean def) {
        return config.get(CATEGORY_NAME, HIDE_NAMETAGS_ENTRY, def).getBoolean();
    }

    private static int getSkyColorFromConfig(Configuration config) {
        return getSkyColorFromConfig(config, -1);
    }

    private static int getSkyColorFromConfig(Configuration config, int def) {
        return config.get(CATEGORY_NAME, SKY_COLOR_ENTRY, def).getInt();
    }

    private static int getWidthFromConfig(Configuration config) {
        return getWidthFromConfig(config, Minecraft.getMinecraft().displayWidth);
    }

    private static int getWidthFromConfig(Configuration config, int def) {
        return config.get(CATEGORY_NAME, WIDTH_ENTRY, def).getInt();
    }

    private static int getHeightFromConfig(Configuration config) {
        return getHeightFromConfig(config, Minecraft.getMinecraft().displayHeight);
    }

    private static int getHeightFromConfig(Configuration config, int def) {
        return config.get(CATEGORY_NAME, HEIGHT_ENTRY, def).getInt();
    }

    private static boolean getHighPerformanceFromConfig(Configuration config) {
        return getHighPerformanceFromConfig(config, false);
    }

    private static boolean getHighPerformanceFromConfig(Configuration config, boolean def) {
        return config.get(CATEGORY_NAME, HIGH_PERFORMANCE_ENTRY, def).getBoolean();
    }

    private static String getExportCommandFromConfig(Configuration config) {
        return getExportCommandFromConfig(config, "ffmpeg");
    }

    private static String getExportCommandFromConfig(Configuration config, String def) {
        return config.get(CATEGORY_NAME, EXPORT_COMMAND_ENTRY, def).getString();
    }

    private static String getExportCommandArgsFromConfig(Configuration config) {
        return getExportCommandArgsFromConfig(config, "");
    }

    private static String getExportCommandArgsFromConfig(Configuration config, String def) {
        return config.get(CATEGORY_NAME, EXPORT_COMMAND_ARGS_ENTRY, def).getString();
    }
}