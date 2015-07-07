package eu.crushedpixel.replaymod.assets;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssetFileUtils {

    //make sure to update these methods when adding new implementations of the ReplayAsset interface

    public static String[] fileExtensionsForAssetClass(Class<? extends ReplayAsset> clazz) {
        if(clazz == ReplayImageAsset.class) {
            return ImageIO.getReaderFileSuffixes();
        }

        return null;
    }

    public static String[] getAllAvailableExtensions() {
        List<String> extensions = new ArrayList<String>();

        extensions.addAll(Arrays.asList(fileExtensionsForAssetClass(ReplayImageAsset.class)));

        return extensions.toArray(new String[extensions.size()]);
    }
}
