package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.resources.I18n;

@AllArgsConstructor
public enum EncodingPreset implements GuiEntryListEntry {

    MP4CUSTOM("replaymod.gui.rendersettings.presets.mp4.custom",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libx264 -b:v %BITRATE% -pix_fmt yuv420p %FILENAME%.mp4", "mp4"),

    MP4HIGH("replaymod.gui.rendersettings.presets.mp4.high",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libx264 -preset ultrafast -qp 1 -pix_fmt yuv420p %FILENAME%.mp4", "mp4"),

    MP4DEFAULT("replaymod.gui.rendersettings.presets.mp4.default",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libx264 -preset ultrafast -pix_fmt yuv420p %FILENAME%.mp4", "mp4"),

    MP4POTATO("replaymod.gui.rendersettings.presets.mp4.potato",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libx264 -preset ultrafast -crf 51 -pix_fmt yuv420p %FILENAME%.mp4", "mp4"),

    WEBMCUSTOM("replaymod.gui.rendersettings.presets.webm.custom",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libvpx -b:v %BITRATE% %FILENAME%.webm", "webm"),

    MKVLOSSLESS("replaymod.gui.rendersettings.presets.mkv.lossless",
            "-f rawvideo -pix_fmt argb -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an -c:v libx264 -preset ultrafast -qp 0 %FILENAME%.mkv", "mkv");

    private String name;

    @Getter
    private String commandLineArgs;

    @Getter
    private String fileExtension;

    public boolean hasBitrateSetting() {
        return commandLineArgs.contains("%BITRATE%");
    }

    @Override
    public String getDisplayString() {
        return getI18nName();
    }

    public String getI18nName() {
        return I18n.format(name);
    }
}
