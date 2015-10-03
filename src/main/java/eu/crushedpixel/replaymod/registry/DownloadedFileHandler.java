package eu.crushedpixel.replaymod.registry;

import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class DownloadedFileHandler {

    private HashMap<Integer, File> downloadedFiles = new HashMap<Integer, File>();

    private File downloadFolder;

    public DownloadedFileHandler() {
        try {
            downloadFolder = ReplayFileIO.getReplayDownloadFolder();

            for(File f : FileUtils.listFiles(downloadFolder, new String[]{"mcpr"}, false)) {
                try {
                    int id = Integer.parseInt(FilenameUtils.getBaseName(f.getAbsolutePath()));
                    downloadedFiles.put(id, f);
                } catch(NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File generateFileForID(int id) {
        return new File(downloadFolder, id+ ".zip");
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

    public File downloadFileForID(int id, ProgressUpdateListener progressUpdateListener) {
        File f = getFileForID(id);
        if(f != null) return f;

        f = generateFileForID(id);

        try {
            ReplayMod.apiClient.downloadFile(id, f, progressUpdateListener);
            if(f.exists()) {
                addToIndex(id);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return f;
    }

    public HashMap<Integer, File> getDownloadedFiles() {
        return downloadedFiles;
    }
}
