package com.replaymod.online.api.replay.pagination;

import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.api.replay.holders.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DownloadedFilePagination implements Pagination {

    private final ReplayModOnline mod;
    private int page;
    private HashMap<Integer, FileInfo> files = new HashMap<>();

    public DownloadedFilePagination(ReplayModOnline mod) {
        this.mod = mod;
        this.page = -1;
    }

    @Override
    public List<FileInfo> getFiles() {
        return new ArrayList<>(files.values());
    }

    @Override
    public int getLoadedPages() {
        return page;
    }

    @Override
    public boolean fetchPage() {
        page++;

        List<Integer> toAdd = new ArrayList<>();
        for(String fileName : mod.getDownloadsFolder().list()) {
            int i;
            try {
                i = Integer.parseInt(fileName.substring(0, fileName.indexOf('.')));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                continue;
            }
            if(!files.containsKey(i)) {
                toAdd.add(i);
                if(toAdd.size() >= Pagination.PAGE_SIZE) break;
            }
        }

        try {
            FileInfo[] fis = mod.getApiClient().getFileInfo(toAdd);
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
