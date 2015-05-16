package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.utils.ReplayFile;
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
            if(!FilenameUtils.getExtension(f.getAbsolutePath()).equals("mcpr")) continue;
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

    private File generateFileForID(int id) {
        return new File(downloadFolder, id+ReplayFile.ZIP_FILE_EXTENSION);
    }

    public void addToIndex(int id) {
        try {
            File f = generateFileForID(id);
            if(f.exists()) downloadedFiles.put(id, f);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public File getFileForID(int id) {
        return downloadedFiles.get(id);
    }

    public File downloadFileForID(int id) {
        File f = getFileForID(id);
        if(f != null) return f;

        f = generateFileForID(id);

        try {
            ReplayMod.apiClient.downloadFile(AuthenticationHandler.getKey(), id, f);
            addToIndex(id);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return f;
    }

    public HashMap<Integer, File> getDownloadedFiles() {
        return downloadedFiles;
    }
}
