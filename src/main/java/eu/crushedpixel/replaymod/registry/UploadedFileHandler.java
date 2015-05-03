package eu.crushedpixel.replaymod.registry;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UploadedFileHandler {

    private Configuration configuration;

    public UploadedFileHandler(File confDir) {
        try {
            File confFile = new File(confDir, "uploaded");
            confFile.createNewFile();
            configuration = new Configuration(confFile);
            configuration.load();

            Property uploaded = configuration.get("uploaded", "hashes", new String[0]);

            configuration.save();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void markAsUploaded(File file) throws IOException {
        String checksum = String.valueOf(FileUtils.checksumCRC32(file));
        List<String> hashes = new ArrayList<String>(Arrays.asList(configuration.get("uploaded",
                "hashes", new String[0]).getStringList()));
        configuration.removeCategory(configuration.getCategory("uploaded"));

        hashes.add(checksum);

        configuration.get("uploaded", "hashes", hashes.toArray(new String[hashes.size()]));
        configuration.save();
    }

    public boolean isUploaded(File file) {
        try {
            String checksum = String.valueOf(FileUtils.checksumCRC32(file));
            List<String> hashes = new ArrayList<String>(Arrays.asList(configuration.get("uploaded",
                    "hashes", new String[0]).getStringList()));
            return hashes.contains(checksum);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
