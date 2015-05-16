package eu.crushedpixel.replaymod.api.client.pagination;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DownloadedFilePagination implements Pagination {

    private int page;
    private HashMap<Integer, FileInfo> files = new HashMap<Integer, FileInfo>();

    public DownloadedFilePagination() {
        this.page = -1;
    }

    @Override
    public List<FileInfo> getFiles() {
        return new ArrayList<FileInfo>(files.values());
    }

    @Override
    public int getLoadedPages() {
        return page;
    }

    @Override
    public boolean fetchPage() {
        page++;

        HashMap<Integer, File> f = ReplayMod.downloadedFileHandler.getDownloadedFiles();
        List<Integer> toAdd = new ArrayList<Integer>();
        for(int i : f.keySet()) {
            if(!files.containsKey(i)) {
                toAdd.add(i);
                if(toAdd.size() >= Pagination.PAGE_SIZE) break;
            }
        }

        files.keySet().retainAll(f.keySet());

        try {
            FileInfo[] fis = ReplayMod.apiClient.getFileInfo(toAdd);
            if(fis.length < 1) {
                page--;
                return false;
            }

            for(FileInfo info : fis) {
                files.put(info.getId(), info);
            }

            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        page--;
        return false;
    }
}
