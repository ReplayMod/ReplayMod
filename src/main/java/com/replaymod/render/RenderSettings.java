package com.replaymod.render;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.ReadableColor;

import java.io.File;

@Data
public class RenderSettings {
    public enum RenderMethod {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR, ODS;

        @Override
        public String toString() {
            return I18n.format("replaymod.gui.rendersettings.renderer." + name().toLowerCase());
        }

        public String getDescription() {
            return I18n.format("replaymod.gui.rendersettings.renderer." + name().toLowerCase() + ".description");
        }
    }

    public enum EncodingPreset {
        MP4_CUSTOM("-an -c:v libx264 -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        MP4_HIGH("-an -c:v libx264 -preset ultrafast -qp 1 -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        MP4_DEFAULT("-an -c:v libx264 -preset ultrafast -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        MP4_POTATO("-an -c:v libx264 -preset ultrafast -crf 51 -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        WEBM_CUSTOM("-an -c:v libvpx -b:v %BITRATE% \"%FILENAME%\"", "webm"),

        MKV_LOSSLESS("-an -c:v libx264 -preset ultrafast -qp 0 \"%FILENAME%\"", "mkv"),

        PNG("\"%FILENAME%-%06d.png\"", "png");

        private final String preset;
        private final String fileExtension;

        EncodingPreset(String preset, String fileExtension) {
            this.preset = preset;
            this.fileExtension = fileExtension;
        }

        public String getValue() {
            return "-y -f rawvideo -pix_fmt rgb24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - %FILTERS%" + preset;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public boolean hasBitrateSetting() {
            return preset.contains("%BITRATE%");
        }

        public boolean isYuv420() { return preset.contains("-pix_fmt yuv420p"); }

        @Override
        public String toString() {
            return I18n.format("replaymod.gui.rendersettings.presets." + name().replace('_', '.').toLowerCase());
        }
    }

    public enum ObservationPreset {
        NONE("",""),
        DEFAULT("inventory", "json"),
        JSON_FULL("full", "json"),
        JSON_MAIN_INVENTORY("inventory", "json"),
        JSON_HOTBAR("hotbar", "json"),

        RAW_FULL("full", "bin"),
        RAW_MAIN_INVENTORY("inventory", "json"),
        RAW_HOTBAR("hotbar", "bin");

        public static final int[] slotIds = 
        { 0,  1,  2,  3,  4,  5,  6,  7,  8, // Hotbar
          9, 10, 11, 12, 13, 14, 15, 16, 17, // Main Inv
         18, 19, 20, 21, 22, 23, 24, 25, 26, // Main Inv
         27, 28, 29, 30, 31, 32, 33, 34, 35, // Main Inv
        100,    101,    102,    103,   -106};//Armor + Off hand （full)

        private static final int[] invSizes = {
            9,   // hotbar
            36,  // inventory
            41}; // full

        private static final int hotbar_len = 9;
        private static final int inventory_len = 36;
        private static final int full_len = 41;


        private final String type;
        private final String fileExtension;

        ObservationPreset(String preset, String fileExtension) {
            this.type = preset;
            this.fileExtension = fileExtension;
        }

        public int getSize() {
            if (type.contains("full")) return 41;
            if (type.contains("hotbar")) return 9;
            if (type.contains("inventory")) return 36;
            return 0; //Type unknown
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public boolean isJSON() {
            return type.contains("json");
        }

        @Override
        public String toString() {
            return I18n.format("replaymod.gui.rendersettings.presets." + name().replace('_', '.').toLowerCase());
        }
    }

    @AllArgsConstructor
    public enum AntiAliasing {
        NONE(1), X2(2), X4(4), X8(8);

        @Getter
        private final int factor;

        @Override
        public String toString() {
            return I18n.format("replaymod.gui.rendersettings.antialiasing." + name().toLowerCase());
        }
    }

    private final RenderMethod renderMethod;
    private final EncodingPreset encodingPreset;
    private final ObservationPreset observationPreset;
    private final int videoWidth;
    private final int videoHeight;
    private final int framesPerSecond;
    private final int bitRate;
    private final File outputFile;
    private final File observationFile;

    private final boolean renderNameTags;
    private final boolean stabilizeYaw;
    private final boolean stabilizePitch;
    private final boolean stabilizeRoll;
    private final ReadableColor chromaKeyingColor;
    private final boolean inject360Metadata;
    private final AntiAliasing antiAliasing;

    private final String exportCommand;
    private final String exportArguments;

    private final boolean highPerformance;

    /**
     * @return the width of the output video during rendering, including the upscale for Anti-Aliasing.
     */
    public int getVideoWidth() {
        return videoWidth * antiAliasing.getFactor();
    }

    /**
     * @return the height of the output video during rendering, including the upscale for Anti-Aliasing.
     */
    public int getVideoHeight() {
        return videoHeight * antiAliasing.getFactor();
    }

    /**
     * @return the actual width of the output video.
     */
    public int getTargetVideoWidth() {
        return videoWidth;
    }

    /**
     * @return the actual height of the output video.
     */
    public int getTargetVideoHeight() {
        return videoHeight;
    }

    public String getVideoFilters() {
        if (antiAliasing == AntiAliasing.NONE) {
            return "";
        } else {
            double factor = 1.0 / antiAliasing.getFactor();
            return String.format("-vf scale=iw*%1$s:ih*%1$s ", factor);
        }
    }

}
