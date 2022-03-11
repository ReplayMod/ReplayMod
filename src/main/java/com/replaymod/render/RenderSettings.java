package com.replaymod.render;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.replaymod.core.utils.FileTypeAdapter;
import com.replaymod.core.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Util;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import static com.replaymod.render.ReplayModRender.LOGGER;

public class RenderSettings {
    public enum RenderMethod {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR, ODS, BLEND;

        @Override
        public String toString() {
            return I18n.translate("replaymod.gui.rendersettings.renderer." + name().toLowerCase());
        }

        public String getDescription() {
            return I18n.translate("replaymod.gui.rendersettings.renderer." + name().toLowerCase() + ".description");
        }

        public boolean isSpherical() {
            return this == EQUIRECTANGULAR || this == ODS;
        }

        public boolean hasFixedAspectRatio() {
            return this == EQUIRECTANGULAR || this == ODS || this == CUBIC;
        }

        @SuppressWarnings("RedundantIfStatement")
        public boolean isSupported() {
            //#if MC<10800 || MC>=11500
            if (this == BLEND) {
                return false;
            }
            //#endif

            return true;
        }

        public static RenderMethod[] getSupported() {
            return Arrays.stream(values()).filter(RenderMethod::isSupported).toArray(RenderMethod[]::new);
        }
    }

    public enum EncodingPreset {
        MP4_CUSTOM("-an -c:v libx264 -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        MP4_POTATO("-an -c:v libx264 -preset ultrafast -crf 51 -pix_fmt yuv420p \"%FILENAME%\"", "mp4"),

        WEBM_CUSTOM("-an -c:v libvpx -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"", "webm"),

        MKV_LOSSLESS("-an -c:v libx264 -preset ultrafast -qp 0 \"%FILENAME%\"", "mkv"),

        BLEND(null, "blend"),

        EXR(null, "exr"),

        PNG(null, "png");

        private final String preset;
        private final String fileExtension;

        EncodingPreset(String preset, String fileExtension) {
            this.preset = preset;
            this.fileExtension = fileExtension;
        }

        public String getValue() {
            return "-y -f rawvideo -pix_fmt bgra -s %WIDTH%x%HEIGHT% -r %FPS% -i - %FILTERS%" + preset;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public boolean hasBitrateSetting() {
            return preset != null && preset.contains("%BITRATE%");
        }

        public boolean isYuv420() { return preset != null && preset.contains("-pix_fmt yuv420p"); }

        @Override
        public String toString() {
            return I18n.translate("replaymod.gui.rendersettings.presets." + name().replace('_', '.').toLowerCase());
        }

        public boolean isSupported() {
            if (this == BLEND) {
                return RenderMethod.BLEND.isSupported();
            } else if (this == EXR) {
                // Need LJWGL 3
                //#if MC>=11400
                return true;
                //#else
                //$$ return false;
                //#endif
            } else {
                return true;
            }
        }

        public static EncodingPreset[] getSupported() {
            return Arrays.stream(values()).filter(EncodingPreset::isSupported).toArray(EncodingPreset[]::new);
        }
    }

    public enum AntiAliasing {
        NONE(1), X2(2), X4(4), X8(8);

        private final int factor;

        AntiAliasing(int factor) {
            this.factor = factor;
        }

        public int getFactor() {
            return factor;
        }

        @Override
        public String toString() {
            return I18n.translate("replaymod.gui.rendersettings.antialiasing." + name().toLowerCase());
        }
    }

    private final RenderMethod renderMethod;
    private final EncodingPreset encodingPreset;
    private final int videoWidth;
    private final int videoHeight;
    private final int framesPerSecond;
    private final int bitRate;
    @JsonAdapter(FileTypeAdapter.class)
    private final File outputFile;

    private final boolean renderNameTags;
    private final boolean includeAlphaChannel;
    private final boolean stabilizeYaw;
    private final boolean stabilizePitch;
    private final boolean stabilizeRoll;
    private final Color chromaKeyingColor;
    private final int sphericalFovX;
    private final int sphericalFovY;
    private final boolean injectSphericalMetadata;
    private final boolean depthMap;
    private final boolean cameraPathExport;
    private final boolean afterEffectsCameraPathExport;
    private final AntiAliasing antiAliasing;

    private final String exportCommand;
    // We switched from rgb24 to bgra for performance at one point, so for backwards compatibility we need to
    // reset the arguments if they're from an older version. Easiest way to do that is to just change the key
    // and handle the null during loading.
    @SerializedName("exportArguments")
    // using an empty string as default because old versions will realize it's not a preset and prompt the user
    private final String exportArgumentsPreBgra = "";
    @SerializedName("exportArgumentsBgra")
    private final String exportArguments;

    private final boolean highPerformance;

    public RenderSettings() {
        this(
                RenderSettings.RenderMethod.DEFAULT,
                RenderSettings.EncodingPreset.MP4_CUSTOM,
                1920,
                1080,
                60,
                20 << 20,
                null,
                true,
                false,
                false,
                false,
                false,
                null,
                360,
                180,
                false,
                false,
                false,
                false,
                RenderSettings.AntiAliasing.NONE,
                "",
                RenderSettings.EncodingPreset.MP4_CUSTOM.getValue(),
                false
        );
    }

    public RenderSettings(
            RenderMethod renderMethod,
            EncodingPreset encodingPreset,
            int videoWidth,
            int videoHeight,
            int framesPerSecond,
            int bitRate,
            File outputFile,
            boolean renderNameTags,
            boolean includeAlphaChannel,
            boolean stabilizeYaw,
            boolean stabilizePitch,
            boolean stabilizeRoll,
            ReadableColor chromaKeyingColor,
            int sphericalFovX,
            int sphericalFovY,
            boolean injectSphericalMetadata,
            boolean depthMap,
            boolean cameraPathExport,
            boolean afterEffectsCameraPathExport,
            AntiAliasing antiAliasing,
            String exportCommand,
            String exportArguments,
            boolean highPerformance
    ) {
        this.renderMethod = renderMethod;
        this.encodingPreset = encodingPreset;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.framesPerSecond = framesPerSecond;
        this.bitRate = bitRate;
        this.outputFile = outputFile;
        this.renderNameTags = renderNameTags;
        this.includeAlphaChannel = includeAlphaChannel;
        this.stabilizeYaw = stabilizeYaw;
        this.stabilizePitch = stabilizePitch;
        this.stabilizeRoll = stabilizeRoll;
        this.chromaKeyingColor = chromaKeyingColor == null ? null : new Color(chromaKeyingColor);
        this.sphericalFovX = sphericalFovX;
        this.sphericalFovY = sphericalFovY;
        this.injectSphericalMetadata = injectSphericalMetadata;
        this.depthMap = depthMap;
        this.cameraPathExport = cameraPathExport;
        this.afterEffectsCameraPathExport = afterEffectsCameraPathExport;
        this.antiAliasing = antiAliasing;
        this.exportCommand = exportCommand;
        this.exportArguments = exportArguments;
        this.highPerformance = highPerformance;
    }

    public RenderSettings withEncodingPreset(EncodingPreset encodingPreset) {
        return new RenderSettings(
                renderMethod,
                encodingPreset,
                videoWidth,
                videoHeight,
                framesPerSecond,
                bitRate,
                outputFile,
                renderNameTags,
                includeAlphaChannel,
                stabilizeYaw,
                stabilizePitch,
                stabilizeRoll,
                chromaKeyingColor,
                sphericalFovX,
                sphericalFovY,
                injectSphericalMetadata,
                depthMap,
                cameraPathExport,
                afterEffectsCameraPathExport,
                antiAliasing,
                exportCommand,
                exportArguments,
                highPerformance
        );
    }

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
        StringBuilder filters = new StringBuilder();

        if (antiAliasing != AntiAliasing.NONE) {
            double factor = 1.0 / antiAliasing.getFactor();
            filters.append(String.format("-filter:v scale=iw*%1$s:ih*%1$s ", factor));
        }

        return filters.toString();
    }

    public String getExportCommandOrDefault() {
        return exportCommand.isEmpty() ? findFFmpeg() : exportCommand;
    }

    private static String findFFmpeg() {
        switch (Util.getOperatingSystem()) {
            case WINDOWS:
                // Allow windows users to unpack the ffmpeg archive into a sub-folder of their .minecraft folder
                File dotMinecraft = MCVer.getMinecraft().runDirectory;
                File inDotMinecraft = new File(dotMinecraft, "ffmpeg/bin/ffmpeg.exe");
                if (inDotMinecraft.exists()) {
                    LOGGER.debug("FFmpeg found in .minecraft/ffmpeg");
                    return inDotMinecraft.getAbsolutePath();
                }
                // But a significant amount of people are not even able to follow instruction to do that.
                // Instead they'll regularly put it at `.minecraft/ffmpeg-version/bin/ffmpeg.exe`
                // or `.minecraft/ffmpeg/ffmpeg-version/bin/ffmpeg.exe`
                // So, for support's sake, let's just search the entire .minecraft folder for an `ffmpeg.exe` file.
                // We retain above check to have a fast path for any old installations.
                try {
                    Path[] result = new Path[1];
                    Files.walkFileTree(dotMinecraft.toPath(), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if ("ffmpeg.exe".equals(file.getFileName().toString())) {
                                result[0] = file;
                                return FileVisitResult.TERMINATE;
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
                    if (result[0] != null) {
                        return result[0].toAbsolutePath().toString();
                    }
                } catch (IOException e) {
                    LOGGER.debug("Error searching .minecraft for ffmpeg.exe:", e);
                }
                break;
            case OSX:
                // The PATH doesn't seem to be set as expected on OSX, therefore we check some common locations ourselves
                for (String path : new String[]{"/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg"}) {
                    File file = new File(path);
                    if (file.exists()) {
                        LOGGER.debug("Found FFmpeg at {}", path);
                        return path;
                    } else {
                        LOGGER.debug("FFmpeg not located at {}", path);
                    }
                }
                // Homebrew doesn't seem to reliably symlink its installed binaries either
                // and there's multiple locations for where Homebrew is.
                for (String path : new String[]{"/usr/local", "/opt/homebrew"}) {
                    File homebrewFolder = new File(path + "/Cellar/ffmpeg");
                    String[] homebrewVersions = homebrewFolder.list();
                    if (homebrewVersions == null) {
                        continue;
                    }
                    Optional<File> latestOpt = Arrays.stream(homebrewVersions)
                            .map(ComparableVersion::new) // Convert file name to comparable version
                            .sorted(Comparator.reverseOrder()) // Sort for latest version
                            .map(ComparableVersion::toString) // Convert back to file name
                            .map(v -> new File(new File(homebrewFolder, v), "bin/ffmpeg")) // Convert to binary files
                            .filter(File::exists) // Filter invalid installations (missing executable)
                            .findFirst(); // Take first one
                    if (latestOpt.isPresent()) {
                        File latest = latestOpt.get();
                        LOGGER.debug("Found {} versions of FFmpeg installed with homebrew, chose {}",
                                homebrewVersions.length, latest);
                        return latest.getAbsolutePath();
                    }
                }
                break;
            case LINUX: // Linux users are entrusted to have their PATH configured correctly (most package manager do this)
            case SOLARIS: // Never heard of anyone running this mod on Solaris having any problems
            case UNKNOWN: // Unknown OS, just try to use "ffmpeg"
        }
        LOGGER.debug("Using default FFmpeg executable");
        return "ffmpeg";
    }

    public RenderMethod getRenderMethod() {
        return renderMethod;
    }

    public EncodingPreset getEncodingPreset() {
        return encodingPreset;
    }

    public int getFramesPerSecond() {
        return framesPerSecond;
    }

    public int getBitRate() {
        return bitRate;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean isRenderNameTags() {
        return renderNameTags;
    }

    public boolean isIncludeAlphaChannel() {
        return includeAlphaChannel;
    }

    public boolean isStabilizeYaw() {
        return stabilizeYaw;
    }

    public boolean isStabilizePitch() {
        return stabilizePitch;
    }

    public boolean isStabilizeRoll() {
        return stabilizeRoll;
    }

    public ReadableColor getChromaKeyingColor() {
        return chromaKeyingColor;
    }

    public int getSphericalFovX() {
        return sphericalFovX;
    }

    public int getSphericalFovY() {
        return sphericalFovY;
    }

    public boolean isInjectSphericalMetadata() {
        return injectSphericalMetadata;
    }

    public boolean isDepthMap() {
        return depthMap;
    }

    public boolean isCameraPathExport() {
        return cameraPathExport;
    }

    public boolean isAfterEffectsCameraPathExport() {
        return afterEffectsCameraPathExport;
    }

    public AntiAliasing getAntiAliasing() {
        return antiAliasing;
    }

    public String getExportCommand() {
        return exportCommand;
    }

    public String getExportArguments() {
        return exportArguments;
    }

    public boolean isHighPerformance() {
        return highPerformance;
    }

    @Override
    public String toString() {
        return "RenderSettings{" +
                "renderMethod=" + renderMethod +
                ", encodingPreset=" + encodingPreset +
                ", videoWidth=" + videoWidth +
                ", videoHeight=" + videoHeight +
                ", framesPerSecond=" + framesPerSecond +
                ", bitRate=" + bitRate +
                ", outputFile=" + outputFile +
                ", renderNameTags=" + renderNameTags +
                ", includeAlphaChannel=" + includeAlphaChannel +
                ", stabilizeYaw=" + stabilizeYaw +
                ", stabilizePitch=" + stabilizePitch +
                ", stabilizeRoll=" + stabilizeRoll +
                ", chromaKeyingColor=" + chromaKeyingColor +
                ", sphericalFovX=" + sphericalFovX +
                ", sphericalFovY=" + sphericalFovY +
                ", injectSphericalMetadata=" + injectSphericalMetadata +
                ", depthMap=" + depthMap +
                ", cameraPathExport=" + cameraPathExport +
                ", afterEffectscameraPathExport=" + afterEffectsCameraPathExport +
                ", antiAliasing=" + antiAliasing +
                ", exportCommand='" + exportCommand + '\'' +
                ", exportArgumentsPreBgra='" + exportArgumentsPreBgra + '\'' +
                ", exportArguments='" + exportArguments + '\'' +
                ", highPerformance=" + highPerformance +
                '}';
    }
}
