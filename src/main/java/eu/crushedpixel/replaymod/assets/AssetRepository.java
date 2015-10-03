package eu.crushedpixel.replaymod.assets;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@EqualsAndHashCode
public class AssetRepository {

    private final Map<UUID, ReplayAsset> replayAssets;

    public AssetRepository() {
        replayAssets = new HashMap<UUID, ReplayAsset>();
    }

    public AssetRepository(AssetRepository toCopy) {
        HashMap<UUID, ReplayAsset> newAssetList = new HashMap<UUID, ReplayAsset>();

        for(Map.Entry<UUID, ReplayAsset> e : toCopy.replayAssets.entrySet()) {
            newAssetList.put(e.getKey(), e.getValue().copy());
        }

        this.replayAssets = newAssetList;
    }

    public ReplayAsset addAsset(String assetFileName, InputStream inputStream) throws IOException {
        return addAsset(assetFileName, inputStream, null);
    }

    public ReplayAsset addAsset(String assetFileName, InputStream inputStream, UUID uuid) throws IOException {

        if(uuid == null) {
            uuid = UUID.randomUUID();
        } else {
            assetFileName = assetFileName.replace(uuid.toString()+"_", "");
        }

        ReplayAsset asset = assetFromFileName(assetFileName);

        asset.loadFromStream(inputStream);

        replayAssets.put(uuid, asset);

        return asset;
    }

    public void removeAsset(ReplayAsset asset) {
        for(Map.Entry<UUID, ReplayAsset> e :
                new HashSet<Map.Entry<UUID, ReplayAsset>>(replayAssets.entrySet()))
            if(e.getValue().equals(asset))
                replayAssets.remove(e.getKey());
    }

    public void saveAssets() {
        // TODO
//        try {
//            ReplayFile replayFile = ReplayHandler.getReplayFile();
//
//            for (ReplayAssetEntry entry : replayFile.getAssets()) {
//                if (!replayAssets.containsKey(entry.getUuid())) {
//                    replayFile.removeAsset(entry.getUuid());
//                }
//            }
//
//            for(Map.Entry<UUID, ReplayAsset> e : replayAssets.entrySet()) {
//                UUID uuid = e.getKey();
//                ReplayAsset asset = e.getValue();
//                String extension = asset.getSavedFileExtension();
//                String name = asset.getAssetName();
//
//                try (OutputStream out = replayFile.writeAsset(new ReplayAssetEntry(uuid, extension, name))) {
//                    asset.writeToStream(out);
//                }
//            }
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
    }

    public static ReplayAsset assetFromFileName(String filename) {
        String baseName = FilenameUtils.getBaseName(filename);
        String extension = FilenameUtils.getExtension(filename);

        if(ArrayUtils.contains(AssetFileUtils.fileExtensionsForAssetClass(ReplayImageAsset.class), extension)) {
            return new ReplayImageAsset(baseName);
        }

        throw new UnsupportedOperationException("Can't create ReplayAsset from File "+filename);
    }

    public List<ReplayAsset> getCopyOfReplayAssets() {
        return new ArrayList<ReplayAsset>(replayAssets.values());
    }

    public ReplayAsset getAssetByUUID(UUID uuid) {
        return replayAssets.get(uuid);
    }

    public UUID getUUIDForAsset(ReplayAsset asset) {
        for(Map.Entry<UUID, ReplayAsset> e : replayAssets.entrySet()) {
            if(e.getValue().equals(asset)) {
                return e.getKey();
            }
        }

        return null;
    }

}
