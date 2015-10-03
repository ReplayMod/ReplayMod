package eu.crushedpixel.replaymod.api.replay.pagination;

import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FavoritedFilePagination implements Pagination {

    private int page;
    private HashMap<Integer, FileInfo> files = new HashMap<Integer, FileInfo>();

    public FavoritedFilePagination() {
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

        try {
            List<Integer> f = ReplayMod.favoritedFileHandler.getFavorited();
            List<Integer> toAdd = new ArrayList<Integer>();
            for(int i : f) {
                if(!files.containsKey(i)) {
                    toAdd.add(i);
                    if(toAdd.size() >= Pagination.PAGE_SIZE) break;
                }
            }

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
