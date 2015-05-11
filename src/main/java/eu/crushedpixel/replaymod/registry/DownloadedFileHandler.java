package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.HashMap;

public class DownloadedFileHandler {

    private HashMap<Integer, File> downloadedFiles = new HashMap<Integer, File>();

    private File downloadFolder;

    public DownloadedFileHandler() {
        downloadFolder = new File(ReplayMod.replaySettings.getDownloadPath());
        downloadFolder.mkdirs();

        for(File f : downloadFolder.listFiles()) {
            try {
                Integer i = Integer.valueOf(FilenameUtils.getBaseName(f.getAbsolutePath()));
                if(i != null) {
                    downloadedFiles.put(i, f);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addToIndex(int id) {
        try {
            File f = new File(downloadFolder, id+"."+ConnectionEventHandler.ZIP_FILE_EXTENSION);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public File getFileForID(int id) {
        return downloadedFiles.get(id);
    }
}
