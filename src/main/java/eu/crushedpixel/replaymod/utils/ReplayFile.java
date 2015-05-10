package eu.crushedpixel.replaymod.utils;

import com.google.common.base.Supplier;
import com.google.gson.Gson;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ReplayFile extends ZipFile {

    public static final String TEMP_FILE_EXTENSION = ".tmcpr";
    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String ZIP_FILE_EXTENSION = ".mcpr";
    public static final String ENTRY_RECORDING = "recording" + TEMP_FILE_EXTENSION;
    public static final String ENTRY_METADATA = "metaData" + JSON_FILE_EXTENSION;
    public static final String ENTRY_PATHS = "paths";
    public static final String ENTRY_THUMB = "thumb";

    private final File file;

    public ReplayFile(File f) throws IOException {
        super(f);
        this.file = f;
    }

    public File getFile() {
        return file;
    }

    public ZipArchiveEntry recordingEntry() {
        return getEntry(ENTRY_RECORDING);
    }

    public Supplier<InputStream> recording() {
        return new Supplier<InputStream>() {
            @Override
            public InputStream get() {
                try {
                    return getInputStream(recordingEntry());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public ZipArchiveEntry metadataEntry() {
        return getEntry(ENTRY_METADATA);
    }

    public Supplier<ReplayMetaData> metadata() {
        return new Supplier<ReplayMetaData>() {
            @Override
            public ReplayMetaData get() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(metadataEntry())));
                    return new Gson().fromJson(reader, ReplayMetaData.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public ZipArchiveEntry pathsEntry() {
        return getEntry(ENTRY_PATHS);
    }

    public Supplier<KeyframeSet[]> paths() {
        return new Supplier<KeyframeSet[]>() {
            @Override
            public KeyframeSet[] get() {
                try {
                    ZipArchiveEntry entry = pathsEntry();
                    if (entry == null) {
                        return null;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(entry)));
                    return new Gson().fromJson(reader, KeyframeSet[].class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public ZipArchiveEntry thumbEntry() {
        return getEntry(ENTRY_THUMB);
    }

    public Supplier<BufferedImage> thumb() {
        return new Supplier<BufferedImage>() {
            @Override
            public BufferedImage get() {
                try {
                    ZipArchiveEntry entry = thumbEntry();
                    if (entry == null) {
                        return null;
                    }
                    InputStream is = getInputStream(entry);
                    int i = 7;
                    while (i > 0) {
                        i -= is.skip(i);  // Security through obscurity \o/
                    }
                    return ImageIO.read(is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
