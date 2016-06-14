package com.replaymod.render;

import lombok.Data;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.ReadableColor;

import java.io.File;

@Data
public class RenderSettings {
    public enum RenderMethod {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR;

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
            return "-y -f rawvideo -pix_fmt rgb24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - " + preset;
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

    private final RenderMethod renderMethod;
    private final EncodingPreset encodingPreset;
    private final int videoWidth;
    private final int videoHeight;
    private final int framesPerSecond;
    private final int bitRate;
    private final File outputFile;

    private final boolean renderNameTags;
    private final boolean stabilizeYaw;
    private final boolean stabilizePitch;
    private final boolean stabilizeRoll;
    private final ReadableColor chromaKeyingColor;
    private final boolean inject360Metadata;

    private final String exportCommand;
    private final String exportArguments;

    private final boolean highPerformance;
}
