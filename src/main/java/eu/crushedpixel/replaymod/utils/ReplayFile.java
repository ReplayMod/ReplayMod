package eu.crushedpixel.replaymod.utils;

import com.google.common.base.Supplier;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.crushedpixel.replaymod.assets.AssetRepository;
import eu.crushedpixel.replaymod.assets.CustomObjectRepository;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.holders.Marker;
import eu.crushedpixel.replaymod.holders.PlayerVisibility;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ReplayFile extends ZipFile {

    public static final String TEMP_FILE_EXTENSION = ".tmcpr";
    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String ZIP_FILE_EXTENSION = ".mcpr";
    public static final String ENTRY_RECORDING = "recording" + TEMP_FILE_EXTENSION;
    public static final String ENTRY_METADATA = "metaData" + JSON_FILE_EXTENSION;
    public static final String ENTRY_PATHS_OLD = "paths";
    public static final String ENTRY_PATHS = "paths" + JSON_FILE_EXTENSION;
    public static final String ENTRY_THUMB = "thumb";
    public static final String ENTRY_RESOURCE_PACK = "resourcepack/%s.zip";
    public static final String ENTRY_RESOURCE_PACK_INDEX = "resourcepack/index"+JSON_FILE_EXTENSION;
    public static final String ENTRY_VISIBILITY_OLD = "visibility";
    public static final String ENTRY_VISIBILITY = "visibility" + JSON_FILE_EXTENSION;
    public static final String ENTRY_MARKERS = "markers" + JSON_FILE_EXTENSION;
    public static final String ENTRY_ASSET_FOLDER = "asset/";
    public static final String ENTRY_CUSTOM_OBJECTS = "objects"+JSON_FILE_EXTENSION;

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
        ZipArchiveEntry newEntry = getEntry(ENTRY_PATHS);
        return newEntry != null ? newEntry : getEntry(ENTRY_PATHS_OLD);
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

    public ZipArchiveEntry visibilityEntry() {
        ZipArchiveEntry newEntry = getEntry(ENTRY_VISIBILITY);
        return newEntry != null ? newEntry : getEntry(ENTRY_VISIBILITY_OLD);
    }

    public Supplier<PlayerVisibility> visibility() {
        return new Supplier<PlayerVisibility>() {
            @Override
            public PlayerVisibility get() {
                try {
                    ZipArchiveEntry entry = visibilityEntry();
                    if (entry == null) {
                        return null;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(entry)));
                    return new Gson().fromJson(reader, PlayerVisibility.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public ZipArchiveEntry markersEntry() {
        return getEntry(ENTRY_MARKERS);
    }

    public Supplier<List<Keyframe<Marker>>> markers() {
        return new Supplier<List<Keyframe<Marker>>>() {
            @Override
            public List<Keyframe<Marker>> get() {
                try {
                    ZipArchiveEntry entry = markersEntry();
                    if (entry == null) {
                        return null;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(entry)));

                    Type keyframeType = new TypeToken<ArrayList<Keyframe<Marker>>>(){}.getType();

                    return new Gson().fromJson(reader, keyframeType);
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

    public ZipArchiveEntry resourcePackIndexEntry() {
        return getEntry(ENTRY_RESOURCE_PACK_INDEX);
    }

    public Supplier<Map<Integer, String>> resourcePackIndex() {
        return new Supplier<Map<Integer, String>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Map<Integer, String> get() {
                try {
                    ZipArchiveEntry entry = resourcePackIndexEntry();
                    if (entry == null) {
                        return null;
                    }
                    Map<Integer, String> index = new HashMap<Integer, String>();
                    JsonObject json = new Gson().fromJson(new InputStreamReader(getInputStream(entry)), JsonObject.class);
                    for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                        index.put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
                    }
                    return index;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public ZipArchiveEntry resourcePackEntry(String hash) {
        return getEntry(String.format(ENTRY_RESOURCE_PACK, hash));
    }

    public Supplier<InputStream> resourcePack(final String hash) {
        return new Supplier<InputStream>() {
            @Override
            public InputStream get() {
                try {
                    ZipArchiveEntry entry = resourcePackEntry(hash);
                    if (entry == null) {
                        return null;
                    }
                    return getInputStream(entry);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public Supplier<AssetRepository> assetRepository() {
        return new Supplier<AssetRepository>() {
            @Override
            @SuppressWarnings("unchecked")
            public AssetRepository get() {
                AssetRepository assetRepository = new AssetRepository();

                Enumeration<ZipArchiveEntry> entries = getEntries();

                while(entries.hasMoreElements()) {

                    try {
                        ZipArchiveEntry entry = entries.nextElement();

                        String entryName = entry.getName();

                        if(entryName.startsWith(ENTRY_ASSET_FOLDER)) {
                            String name = entry.getName().substring(ENTRY_ASSET_FOLDER.length());

                            String[] split = name.split("_");
                            UUID uuid = UUID.fromString(split[0]);

                            assetRepository.addAsset(name, getInputStream(entry), uuid);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                return assetRepository;
            }
        };
    }

    public ZipArchiveEntry customObjectsEntry() {
        return getEntry(ENTRY_CUSTOM_OBJECTS);
    }

    public Supplier<CustomObjectRepository> customImageObjects() {
        return new Supplier<CustomObjectRepository>() {
            @Override
            public CustomObjectRepository get() {
                try {
                    ZipArchiveEntry entry = customObjectsEntry();
                    if(entry == null) return null;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(entry)));
                    return new Gson().fromJson(reader, CustomObjectRepository.class);
                } catch(Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
    }
}
