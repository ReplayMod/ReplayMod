package eu.crushedpixel.replaymod.assets;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetRepository {

    private List<ReplayAsset> replayAssets;

    public AssetRepository() {
        replayAssets = new ArrayList<ReplayAsset>();
    }

    public ReplayAsset addAsset(String assetFileName, InputStream inputStream) throws IOException {
        ReplayAsset asset = assetFromFileName(assetFileName);

        asset.loadFromStream(inputStream);

        replayAssets.add(asset);

        return asset;
    }

    public void removeAsset(ReplayAsset asset) {
        replayAssets.remove(asset);
    }

    public void saveAssets() {
        for(ReplayAsset asset : replayAssets) {
            try {
                String filepath = ReplayFile.ENTRY_ASSET_FOLDER + asset.getDisplayString() + "." + asset.getSavedFileExtension();

                File toAdd = File.createTempFile(asset.getDisplayString(), asset.getSavedFileExtension());
                FileOutputStream fos = new FileOutputStream(toAdd);
                asset.writeToStream(fos);

                ReplayMod.replayFileAppender.registerModifiedFile(toAdd, filepath, ReplayHandler.getReplayFile());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static ReplayAsset assetFromFileName(String filename) {
        String baseName = FilenameUtils.getBaseName(filename);
        String extension = FilenameUtils.getExtension(filename);

        if(ArrayUtils.contains(AssetFileUtils.fileExtensionsForAssetClass(ReplayImageAsset.class), extension)) {
            return new ReplayImageAsset(baseName);
        }

        throw new UnsupportedOperationException("Can't create ReplayAsset from File "+filename);
    }



}
